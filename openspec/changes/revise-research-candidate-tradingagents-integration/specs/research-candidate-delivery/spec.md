# Spec Delta

## Purpose

定义 QuantPF 研究与候选发现模块从用户触发、服务端确定性处理、Dify 编排到按需调用 TradingAgents 的受控行为，使研究结果可回放、可核验、可审核且不会越权变成事实或交易指令。

## ADDED Requirements

### Requirement: 研究工作流由 Spring Boot 管理并按需路由多智能体
系统 SHALL 由 Spring Boot 创建研究任务、冻结输入快照并调用已登记发布的 Dify Workflow；Dify SHALL 按任务类型路由至 TradingAgents，TradingAgents SHALL 仅用于配置允许的复杂多视角研究，非必经步骤。

#### Scenario: 简单摘要不调用 TradingAgents
- **WHEN** 用户提交摘要、格式化、基础事实抽取或研究卡解释任务
- **THEN** Spring Boot 调用对应 Dify Workflow，Workflow 不创建 TradingAgents run，且任务仍可正常完成

#### Scenario: 复杂影响链调用 TradingAgents
- **WHEN** 任务被授权为复杂影响链、多空辩论或反方深度研究且其 Workflow Profile 启用 TradingAgents
- **THEN** Dify 通过登记的内部 Adapter 创建一次有界 TradingAgents run，并把 run 标识及研究草稿纳入最终响应

#### Scenario: 未发布 Workflow 阻断
- **WHEN** 任务绑定的 Dify Workflow 未登记、未发布或环境版本不匹配
- **THEN** Spring Boot 在调用前拒绝该任务并记录原因，不创建 Dify 或 TradingAgents run

### Requirement: 多智能体通信必须经过受控 Adapter
系统 SHALL 使用 HTTPS REST/SSE 与 Dify Workflow 通信，并由 Dify 经内网受控 TradingAgents Adapter 使用 REST/JSON 调用 TradingAgents；浏览器不得直接访问 Dify、Adapter 或 TradingAgents，MCP 不得作为一期主链路。

#### Scenario: Adapter 认证及契约校验
- **WHEN** Dify 请求 TradingAgents 研究
- **THEN** Adapter 校验服务身份、允许的任务类型、输入 Schema、超时和幂等键后才创建 run，并返回结构化 JSON 与运行元数据

#### Scenario: 拒绝任意工具或地址
- **WHEN** Workflow 请求未登记的工具、任意 URL、数据库访问或超出白名单的研究动作
- **THEN** Adapter/网络策略拒绝请求并留下安全审计，TradingAgents 不获得业务库、账户、风险或订单访问权

#### Scenario: TradingAgents 超时或失败
- **WHEN** Adapter 返回超时、限流或引擎错误
- **THEN** Dify 将多智能体步骤标为失败或可恢复降级并返回错误/不完整标记，Spring Boot 保留任务状态与调用关联，不伪造多智能体结论

### Requirement: AI 与多智能体结果只作为待校验草稿
系统 SHALL 将 Dify 和 TradingAgents 输出保存为 AI_DRAFT，并由 Spring Boot 校验 Schema、来源引用、点时、授权、标的身份、冲突及安全边界；Dify/TradingAgents MUST NOT 直接写入核验事实、评分、硬门禁、候选、研究卡权威状态、审核或交接表。

#### Scenario: 引用无法回源
- **WHEN** 多智能体或 Dify 草稿包含无法映射到允许材料/结构化快照的事实引用
- **THEN** 对应主张标记待核验或拒绝，不进入 VERIFIED 事实、有效证据、评分输入或已批准研究卡

#### Scenario: 模型返回评分或概率
- **WHEN** Dify/TradingAgents 输出自报评分、概率、硬门禁结论或机会状态
- **THEN** Spring Boot 忽略这些权威字段或仅作为明示草稿展示，确定性字段仍由其自身服务计算

#### Scenario: 多智能体观点存在分歧
- **WHEN** 不同研究角色对方向、证据或风险存在分歧
- **THEN** 系统保留各角色原始观点、引用、版本和分歧，不得由 Dify 静默抹平为一致结论

### Requirement: 确定性筛选、评分及候选状态由 Spring Boot 主控
系统 SHALL 由 Spring Boot 执行身份/质量/授权等硬门禁、确定性因子与筛选、模块二 70 分制研究评分、候选状态机和研究卡版本；TradingAgents 与 Dify 只能提供被校验的研究草稿，不得单方放行候选。

#### Scenario: 硬门禁未通过
- **WHEN** 候选存在授权、身份、数据质量或风险硬门禁失败
- **THEN** 系统记录失败门禁与原因并阻止候选晋级，即使 AI 草稿建议继续研究

#### Scenario: 研究分缺少跨模块维度
- **WHEN** 模块二研究评分用于总分门槛判断
- **THEN** 系统将其限制为四维合计 70 分贡献，不将其冒充总分或单独满足 70/80 总门槛

#### Scenario: 确定性服务与 AI 不可用解耦
- **WHEN** Dify 或 TradingAgents 不可用而输入数据与规则完整
- **THEN** 系统仍运行确定性计算/筛选并展示已有结果，新 AI 分析标记待补充或降级

### Requirement: 候选审核与模块三交接保留人工和业务门禁
系统 SHALL 以版本条件执行事实核验、研究卡审核、受限覆盖和候选交接；交接只能进入模块三端口，不得由 AI 或研究模块创建策略信号、订单、Paper 或真实交易指令。

#### Scenario: 旧版本审核写入
- **WHEN** 用户提交的审核动作携带的研究卡版本已不是当前版本
- **THEN** 系统拒绝写入并返回当前版本以供刷新，且不产生审核状态副作用

#### Scenario: 候选交接被模块三拒绝
- **WHEN** 模块三返回拒绝并要求补充信息
- **THEN** 系统保存带幂等关联的拒绝回执、展示原因并允许从原候选版本补充，不创建重复交接副作用

#### Scenario: AI 请求交易动作
- **WHEN** Dify 或 TradingAgents 输出建仓、平仓、下单或修改账户状态的要求
- **THEN** 系统将其作为不受支持内容拒绝，不调用订单、账户、持仓或风险写接口

### Requirement: 研究用户旅程可恢复且 Dify 入口清晰
系统 SHALL 在若依研究菜单提供任务、候选、研究卡、证据核验、研究助手和交接入口；普通用户以低输入完成任务并可恢复运行，所有 AI 入口仅调用 Spring Boot 并显示草稿、进度、错误和降级状态。

#### Scenario: 普通用户提交研究任务
- **WHEN** 用户选择研究对象、问题和周期并提交
- **THEN** 系统创建幂等任务与点时快照，显示进度和下一步，不要求用户填写评分公式、模型或风险参数

#### Scenario: 用户从卡片触发多视角研究
- **WHEN** 有权限用户在研究卡点击“补充多视角/反方研究”
- **THEN** 前端仅向 Spring Boot 提交卡片 ID、版本和请求意图，由服务端确认权限及 Profile 后经 Dify 决定是否调用 TradingAgents

#### Scenario: 页面离开后恢复
- **WHEN** 用户返回一个正在运行或已失败的研究任务
- **THEN** 页面根据 Spring Boot 任务/运行记录恢复状态、关联运行 ID、失败原因和可执行的重试/查看动作，不重复创建非幂等任务

#### Scenario: Dify 服务降级
- **WHEN** Dify 达到超时或熔断阈值
- **THEN** 页面保留确定性结果，显示“暂不可用/稍后重试”，且不展示伪造的 AI 解释

### Requirement: 模块二数据模型有版本、幂等、审计及 Dify 隔离
系统 SHALL 复用模块一研究入口及若依权限表，不复制其业务事实；新增模块二业务表使用应用生成 BIGINT 主键、统一审计字段、约束和可检索索引，并使 Dify/TradingAgents 只获得最小化只读上下文。

#### Scenario: 重复研究或交接请求
- **WHEN** 客户端以相同幂等键重试相同研究任务或候选交接
- **THEN** 系统返回原任务/交接结果且不创建重复副作用

#### Scenario: Dify 获取研究上下文
- **WHEN** Spring Boot 为研究型 Workflow 组装输入
- **THEN** 仅传入完成该任务所需的已授权材料引用、结构化指标、点时与质量状态，不传凭据、账户明细、个人敏感信息或无关原文

#### Scenario: AI 无数据库权限
- **WHEN** Dify 或 TradingAgents 以运行身份尝试连接 QuantPF MySQL/Redis/DuckDB 或业务写 API
- **THEN** 网络与凭据权限拒绝访问，审计记录标识调用身份与拒绝原因
