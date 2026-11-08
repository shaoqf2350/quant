# Spec Delta

## Purpose

定义数据与标的基础模块在引入 TradingAgents 后可验证的权威数据治理、Dify Workflow 编排、多智能体研究、用户交互、审计、安全和降级行为，确保 AI 研究不会越过 Spring Boot 的事实、权限与交易边界。

## ADDED Requirements

### Requirement: Spring Boot 保持模块一唯一权威事实源
系统 SHALL 由 Spring Boot 管理数据源、快照、质量、授权、点时、标的身份、交易日历、行情资格、研究任务和业务状态；Dify 与 TradingAgents 的输出只能作为可追溯 AI 草稿。

#### Scenario: TradingAgents 返回事实候选
- **WHEN** TradingAgents 返回新闻事实候选、方向观点或风险判断
- **THEN** 系统将结果保存为 `AI_DRAFT` 并执行 Schema、引用、时间、授权、身份和冲突校验，不直接修改权威事实、标的、日历、资格或研究事件状态

#### Scenario: AI 输出请求修改基础数据
- **WHEN** Dify 或 TradingAgents 输出包含修改数据源、标的身份、交易日历、行情、资格或订单状态的动作
- **THEN** Spring Boot 拒绝执行这些动作、记录安全警告，并保留原始响应供审计

### Requirement: 生产研究任务遵循统一三方通信主链路
系统 SHALL 以“Spring Boot → Dify Workflow → 按需 TradingAgents Adapter → Dify 汇总 → Spring Boot 校验”为标准生产研究链路。

#### Scenario: 复杂材料需要多智能体研究
- **WHEN** 已授权研究任务的 Workflow 判定需要多空辩论、复杂影响链或反方研究
- **THEN** Dify 通过获准的 TradingAgents Adapter 端点提交结构化任务，并在结果返回后保留角色观点和冲突再生成汇总草稿

#### Scenario: 简单事实抽取
- **WHEN** 任务仅要求事实抽取、摘要、格式转换或结构化状态解释
- **THEN** Dify 在固定 Workflow 内完成任务且不调用 TradingAgents，并在运行元数据中记录实际执行路线

#### Scenario: 浏览器尝试直连 AI 服务
- **WHEN** 用户从 QuantPF 页面发起研究或问答
- **THEN** 浏览器只调用 Spring Boot API，不获得 Dify 或 TradingAgents URL、API Key 或服务端凭据

### Requirement: Spring Boot 使用受控 Dify Workflow API
系统 SHALL 通过 HTTPS 调用已登记和已发布的 Dify Workflow，并默认消费 `text/event-stream`，业务代码不得直接依赖 Dify 原始事件名称。

#### Scenario: Dify 流式运行成功
- **WHEN** Spring Boot 使用服务端 Bearer Key 调用 `/v1/workflows/run` 且 `response_mode=streaming`
- **THEN** Dify 事件适配器将开始、节点、片段、完成事件映射为统一运行状态，关联 `invocationId` 与 `difyRunId` 后保存最终草稿

#### Scenario: Dify 流连接中断
- **WHEN** SSE 在 Workflow 完成前中断
- **THEN** 系统使用原 `invocationId`、`difyRunId` 和幂等键查询或恢复运行，且不会重复创建研究任务、草稿或研究事件

### Requirement: Dify 通过 TradingAgents Adapter 调用研究引擎
系统 SHALL 以内部 HTTPS REST/JSON 和版本化 Schema 调用 TradingAgents Adapter；一期不得以 MCP、脚本执行、Python 类直连或数据库访问作为生产主链路。

#### Scenario: 创建异步 TradingAgents 运行
- **WHEN** Dify 提交预计超过安全同步等待时间的多智能体任务
- **THEN** Adapter 返回 `202 Accepted` 和 `tradingAgentsRunId`，Dify 以有界退避查询状态并在超时上限内获取结构化结果

#### Scenario: 请求端点不在白名单
- **WHEN** Workflow 尝试调用未登记 URL 或非获准 TradingAgents 工具
- **THEN** 网关或工具层拒绝请求并记录端点、Workflow版本和安全审计，不执行外部调用

#### Scenario: TradingAgents 返回部分成功
- **WHEN** 一个或多个研究角色失败但其余角色完成
- **THEN** Adapter 返回 `PARTIAL`、已完成观点和缺失角色，Dify 保留缺失影响说明且不得标记为完整辩论

### Requirement: 输入上下文最小化且可追溯
系统 SHALL 仅向 Dify/TradingAgents 提供经授权、点时冻结、用途合格的最小材料和结构化摘要，并关联材料、快照、主体和版本。

#### Scenario: 材料授权不允许 AI 研究
- **WHEN** 材料的授权用途不包含当前研究任务
- **THEN** Spring Boot 在调用 Dify 前返回 `AUTHORIZATION_BLOCKED`，且请求正文和材料内容不离开业务边界

#### Scenario: 输入包含账户或交易凭据
- **WHEN** 上下文组装器检测到账户密钥、订单凭据、数据库连接或无关个人信息
- **THEN** 系统删除或拒绝敏感字段、记录校验结果，并禁止调用 Dify/TradingAgents

#### Scenario: 点时回放研究任务
- **WHEN** 用户按历史 `asOfTime` 重跑研究
- **THEN** 系统只选择当时已可见的材料和结构化版本，并在结果中返回固定的快照与版本引用

### Requirement: 多智能体输出必须经过双层校验
Dify SHALL 执行输出 Schema 和安全初检，Spring Boot SHALL 执行最终 Schema、任务关联、引用、时间、授权、身份、冲突和敏感信息校验。

#### Scenario: 引用超出输入范围
- **WHEN** TradingAgents 或 Dify 返回的引用不属于本次材料快照或无法定位
- **THEN** Spring Boot 将草稿标记为 `CITATION_MISSING` 或 `CLAIM_UNVERIFIED`，阻止事实升级，并向审核用户显示补证据入口

#### Scenario: 输出 Schema 不合法
- **WHEN** AI 结果缺少必填字段、枚举非法或超过大小限制
- **THEN** 系统保存脱敏原始响应和失败原因，将运行标记为 `OUTPUT_INVALID`，且不创建已核验研究事件

#### Scenario: 多空观点互相冲突
- **WHEN** TradingAgents 返回互相冲突的看多、看空和风险观点
- **THEN** Dify 与 Spring Boot 保留冲突、最强反证和证伪条件，不以多数投票替代统计验证或硬风控

### Requirement: AI 运行关系和版本完整可审计
系统 SHALL 关联 `requestId`、`researchTaskId`、`invocationId`、`difyRunId`、可选 `tradingAgentsRunId`、输入快照 Hash，以及 Workflow、Prompt、Schema、模型和知识库版本。

#### Scenario: 运行关联完整
- **WHEN** 一个包含 TradingAgents 的 Workflow 成功结束
- **THEN** 任务详情可查询完整运行链、每段状态、版本、耗时、Token/成本（可获得时）、原始响应引用和验证结果

#### Scenario: 运行引用不一致
- **WHEN** 回调或流式结果中的 request、task、invocation 或 run 标识与持久化记录不一致
- **THEN** 系统拒绝合并结果、记录关联错误，并不得推进研究任务状态

### Requirement: 重复请求和重试保持幂等
系统 SHALL 以任务类型、材料版本、快照 Hash、研究问题和执行版本生成幂等关系，重复调用不得产生业务副作用。

#### Scenario: 用户重复点击深度研究
- **WHEN** 用户在网络超时后使用同一输入和幂等键再次提交
- **THEN** Spring Boot 返回既有任务或运行状态，不创建第二个 TradingAgents 运行或重复研究事件

#### Scenario: 输入材料版本变化
- **WHEN** 同一研究问题的材料版本、授权或 `asOfTime` 发生变化
- **THEN** 系统创建新任务/运行版本且保留旧版本，不复用过期 AI 草稿

### Requirement: 模块一界面提供分角色操作入口
系统 SHALL 在若依 Vue 页面中为管理员、审核用户和普通研究用户提供与权限匹配的数据治理和研究操作，所有 AI 入口均通过 Spring Boot。

#### Scenario: 管理员配置数据源
- **WHEN** 管理员在数据源页面登记、测试或停用来源
- **THEN** 页面显示授权、健康、覆盖、最后成功时间和影响范围；密钥只允许一次性服务端提交且不回显

#### Scenario: 审核用户处理来源冲突
- **WHEN** 审核用户在质量或材料页面确认某个来源版本
- **THEN** 系统要求审核理由，保存操作者、版本和影响范围，重新计算用途资格，并保留未选来源和原始快照

#### Scenario: 普通用户触发多智能体研究
- **WHEN** 有权限用户在新闻/材料详情或研究任务向导点击“深度研究/多空辩论”
- **THEN** 前端显示输入快照、Dify与TradingAgents阶段状态、可安全离开提示及最终草稿标签，不展示中间片段为已确认事实

#### Scenario: 用户无深度研究权限
- **WHEN** 用户缺少 `quant:research:debate` 权限
- **THEN** 页面隐藏或禁用多智能体入口且服务端仍拒绝手工构造的请求

### Requirement: 数据与 AI 状态在界面中明确分层
系统 SHALL 区分确定性数据状态、AI 运行状态、AI 草稿验证状态和已核验研究事件，且不得以空值或成功色误导用户。

#### Scenario: TradingAgents 正在运行
- **WHEN** 任务处于 `QUEUED` 或 `RUNNING`
- **THEN** 页面显示当前阶段、已完成/待处理角色、开始时间和取消/稍后查看选项，不显示最终结论

#### Scenario: TradingAgents 不可用
- **WHEN** Adapter 熔断或服务健康检查失败
- **THEN** 页面显示“多智能体研究暂不可用”，保留结构化数据与Dify单模型草稿入口，并明确该结果不是多智能体结论

#### Scenario: 草稿通过校验但待人工审核
- **WHEN** Spring Boot 自动校验通过且业务规则要求人工确认
- **THEN** 页面显示 `AI草稿/待审核`、引用、冲突和版本，且不提供直接下单按钮

### Requirement: 数据库存储职责严格隔离
系统 SHALL 使用 MySQL 保存权威元数据和业务状态、DuckDB 保存高容量行情分析数据、MinIO 保存大对象引用；Dify Dataset 和 TradingAgents 存储不得替代上述权威记录。

#### Scenario: Dify 需要行情上下文
- **WHEN** Workflow 需要量价或市场状态解释
- **THEN** Spring Boot 从权威存储计算并裁剪结构化摘要后传入，Dify 不连接 DuckDB 或 MySQL

#### Scenario: 原始 AI 响应超过行内阈值
- **WHEN** Dify/TradingAgents 原始响应大于配置的 MySQL 行内上限
- **THEN** 系统把加密/受控对象写入 MinIO，并在调用与草稿表保存对象引用、Hash、大小和保留策略

### Requirement: 模块一不得通过 AI 绕过交易边界
系统 SHALL 禁止 Dify 与 TradingAgents 调用订单、撤单、开平仓、资金、持仓、风险状态和 Kill Switch 写接口。

#### Scenario: 研究结果包含 BUY 或 SELL 动作
- **WHEN** AI 输出包含可执行交易动作、目标仓位或自动平仓指令
- **THEN** 系统删除执行性字段或拒绝结果、标记安全警告，并只保留研究理由和风险提示供后续人工/策略流程使用

#### Scenario: Workflow 尝试访问 Paper 写接口
- **WHEN** Dify 工具调用请求指向 Paper 或交易写端点
- **THEN** 工具白名单拒绝调用并产生高优先级安全审计，业务订单和持仓保持不变

### Requirement: 故障降级不影响确定性数据链路
系统 SHALL 使采集、去重、质量、授权、点时、标的、日历、行情资格和既有 Paper/风险查询独立于 Dify 与 TradingAgents 可用性。

#### Scenario: Dify 整体不可用
- **WHEN** Dify 健康检查失败或调用熔断
- **THEN** 新 AI 任务进入 `AI_ANALYSIS_PENDING` 或明确失败，确定性数据功能继续运行，系统不伪造研究完成

#### Scenario: TradingAgents 超时
- **WHEN** TradingAgents 在 Workflow 超时上限内未完成
- **THEN** 系统执行有界重试后返回 `RETRYABLE_FAILED` 或明确标识的单模型降级草稿，不冒充完整多智能体结果

### Requirement: 回调和直连例外受到严格控制
系统 SHALL 优先使用 Spring Boot 主动调用与 SSE 消费；可选回调和 Spring Boot 直连 TradingAgents 仅用于已定义的内部场景。

#### Scenario: Dify 签名回调有效
- **WHEN** 内部回调的 HMAC、时间戳、Nonce、来源和运行关联全部通过
- **THEN** 系统仅更新调用记录并将草稿送入校验队列，不直接发布研究事件或修改候选状态

#### Scenario: Spring Boot 执行 TradingAgents 运维查询
- **WHEN** 管理员查询已知 `tradingAgentsRunId` 的健康、状态或取消失控任务
- **THEN** Spring Boot 通过同一 Adapter 执行受审计的管理调用，不将该直连结果静默当成标准 Dify Workflow 结果

### Requirement: 配置与密钥按环境隔离
系统 SHALL 为 dev、test、prod 分离 Dify App、Workflow版本、TradingAgents端点、模型密钥、回调密钥、限额和网络策略，凭据不得进入数据库明文、前端或普通日志。

#### Scenario: 生产环境缺少必需密钥
- **WHEN** 生产启动时 Dify 或 TradingAgents 已启用但认证密钥为空
- **THEN** 应用启动失败或对应AI能力保持禁用并产生明确告警，不以匿名方式调用服务

#### Scenario: 日志包含 Authorization 字段
- **WHEN** HTTP客户端或异常处理器记录 Dify/TradingAgents 请求
- **THEN** 日志过滤器移除或掩码 Authorization、Cookie、签名、正文敏感材料和个人信息

### Requirement: 发布前完成端到端可验证验收
系统 SHALL 通过数据库迁移、后端契约、前端行为、Dify Stub、TradingAgents Stub、安全和既有 Paper 回归测试后方可启用生产入口。

#### Scenario: 端到端复杂新闻研究验收
- **WHEN** 测试用户提交带冲突来源的重大新闻深度研究
- **THEN** 系统可追踪完成 Spring Boot快照→Dify→TradingAgents→Dify→Spring Boot校验→人工审核全链路，并验证任何AI阶段都未直接写权威事实或订单

#### Scenario: 安全回归验收
- **WHEN** 测试尝试浏览器直连、伪造回调、重放Nonce、越权工具调用和注入交易指令
- **THEN** 所有请求均被拒绝或安全降级，密钥不泄漏，权威数据、Paper订单和持仓未被修改
