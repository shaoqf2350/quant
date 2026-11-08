# Spec Delta

## Purpose

定义 Spring Boot 与自托管 Dify Workflow 的受控集成行为：只向 Dify 提供最小、已授权、可回放的研究上下文，可靠消费结构化草稿，并在超时、非法输出、回调重放和服务不可用时保持事实源、风控和交易隔离。

## ADDED Requirements

### Requirement: Dify 调用使用已发布 Workflow 和最小上下文
集成服务 SHALL 只调用已登记、已发布且环境匹配的 Workflow 版本；请求 MUST 包含 requestId、幂等键、研究任务 ID、材料/来源/标的引用、snapshot/as-of 时间、授权/质量状态和 Workflow/Prompt/Schema 版本，不得包含交易凭据、数据库凭据、完整用户敏感信息或无关账户明细。

#### Scenario: 调用未发布 Workflow
- **WHEN** 研究任务指定的 Workflow 版本不是已发布或环境不匹配
- **THEN** Spring Boot 拒绝调用并记录可审计错误，不产生 Dify run 或研究事件

#### Scenario: 研究任务发送到 Dify
- **WHEN** 任务输入通过授权、质量、身份和点时检查
- **THEN** 请求只包含最小研究上下文和可回源引用，服务端凭据从环境配置注入且不出现在请求日志

### Requirement: Dify Workflow 流式响应可恢复
集成服务 SHALL 默认以 Workflow API 的 streaming 模式消费 `text/event-stream`，保存 invocation/run 状态与响应哈希；网络中断、超时或进程重启后，使用同一任务/幂等键恢复或补偿，不得重复创建业务任务或通知副作用。

#### Scenario: 流式响应完成
- **WHEN** Dify 返回完整流式响应并包含结束状态
- **THEN** 调用记录标记成功，保存原始响应引用和结构化草稿，进入后续校验队列

#### Scenario: 流式响应中断
- **WHEN** 消费过程中连接中断
- **THEN** invocation 标记可重试或超时，按退避和最大次数补偿；重复补偿复用同一业务幂等键

### Requirement: 可选回调必须签名且只进入校验队列
如果启用 Dify 回调，服务 SHALL 校验来源白名单、时间戳、Nonce、防重放、`X-Dify-Signature` HMAC-SHA256、requestId/runId 关联和幂等键；回调结果只能进入 AI 草稿校验队列，不得直接更新事实、研究事件、策略、风险或订单。

#### Scenario: 重放回调
- **WHEN** 相同签名、Nonce 或幂等键的回调再次到达
- **THEN** 服务返回原处理结果或幂等响应，不重复保存草稿、不重复发布研究事件

#### Scenario: 签名不合法
- **WHEN** 回调签名、时间窗口或 runId 校验失败
- **THEN** 服务拒绝结果并写安全审计，不改变任何业务状态

### Requirement: AI 草稿执行完整校验链
集成服务 SHALL 在保存 AI 草稿后依次校验 Schema、引用存在性、引用与主张一致性、时间可见性、授权用途、标的身份和来源冲突；任一失败必须保留原始响应与失败原因，但不得将草稿升级为 VERIFIED_FACT、研究事件或交易信号。

#### Scenario: 引用不存在
- **WHEN** AI 草稿引用的 source/material/snapshot 不存在
- **THEN** 草稿标记 `SOURCE_UNRESOLVED`，研究任务进入人工复核或失败状态，不进入候选升级

#### Scenario: 草稿全部通过
- **WHEN** Schema、引用、时间、授权、身份和冲突校验全部通过
- **THEN** 草稿进入可研究状态，由 Spring Boot/人工流程决定是否生成版本化研究事件，Dify不能直接写事件主表

### Requirement: Dify 降级不影响确定性业务
集成服务 SHALL 在 Dify 不可用、限流、认证失败或持续超时时安全降级；原始采集、数据质量、标的/日历、确定性行情、既有研究结果、Paper 和风控继续运行，新 AI 任务进入排队/待分析/可重试状态，不得伪造分析结果。

#### Scenario: Dify 连续不可用
- **WHEN** 调用达到降级阈值
- **THEN** 系统标记 Dify 降级、停止无界重试并保留补偿任务，用户仍能查询结构化数据和已有结果

### Requirement: Dify 权限与网络边界
Dify 集成 SHALL 通过服务端适配器访问 Dify，禁止浏览器直连 Dify；Dify 网络身份不得访问 QuantPF MySQL、DuckDB、Redis、Paper、账户、风险或真实交易通道；所有 Key 只能从环境变量/密钥系统读取。

#### Scenario: 浏览器请求 Dify
- **WHEN** 浏览器尝试使用 Dify API Key 直接请求 Dify
- **THEN** 系统架构和网关策略拒绝该路径，前端只能请求 Spring Boot 研究接口

#### Scenario: Dify 尝试访问业务库
- **WHEN** Dify 服务使用其网络身份访问 QuantPF 数据库或订单接口
- **THEN** 网络白名单/凭据权限拒绝访问，并产生安全审计记录
