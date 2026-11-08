# Spec Delta

## Purpose

定义 QuantPF 模块一研究入口与 Dify 协作域的持久化行为契约：研究材料统一状态机、研究任务与运行记录、Dify Workflow 注册与调用留痕、AI 草稿保存与八步校验状态、研究事件与证据链，保证"新闻→Dify 草稿→Spring Boot 核验→研究事件发布"链路可追溯、可幂等、可降级，且 AI 输出与业务事实严格分离。

## ADDED Requirements

### Requirement: 研究材料统一状态机
系统 SHALL 将新闻、公告、财报、研报、产业数据、规则文档统一为研究材料对象并持久化状态机（RAW→NORMALIZED→DEDUPLICATED→QUALITY_CHECKED→RESEARCH_QUEUED→RESEARCHED→ARCHIVED），关联原始快照、发布/可见/生效/观察/修订五类时间、质量状态、授权状态与去重结论（独立/完全重复/改写/转载引用）；多家转载同一原文 MUST NOT 计为多个独立来源。

#### Scenario: 转载识别
- **WHEN** 三家媒体发布同一原文的转载新闻
- **THEN** 材料层标记为转载引用关系并指向同一原始内容，交叉核验不将其计为三个独立证据

### Requirement: 研究任务幂等与输入快照冻结
系统 SHALL 为每个研究任务持久化任务类型、触发来源、输入引用（材料/来源/主体/行情或特征快照）、时点（published_at/available_at/as_of_time）、授权与质量状态、研究问题与周期、目标 Workflow/Prompt/Schema 版本及幂等键；输入引用不完整时 MUST 创建等待输入状态的任务且 MUST NOT 标记为已完成研究；相同幂等键重复创建 MUST 返回原任务。

#### Scenario: 输入不足
- **WHEN** 创建研究任务时关键材料尚未通过质量校验
- **THEN** 任务进入 `WAITING_FOR_INPUT`，不产生研究结论或有效候选，补齐后可继续

### Requirement: Dify 调用全程留痕
系统 SHALL 为每次 Dify 调用持久化调用记录：QuantPF 侧调用标识、关联任务与运行、Workflow 键与版本、Dify run 标识、请求/响应载荷引用与哈希、状态（含流式消费、超时、限流）、耗时、Token 用量（可得时）、错误码与重试链；请求载荷 MUST NOT 包含交易凭据、密钥明文或与研究无关的个人信息；仅已发布版本的 Workflow MUST 可被调用。

#### Scenario: 超时重试同键恢复
- **WHEN** Dify 调用超时后按同一任务与幂等键重试
- **THEN** 新调用记录通过 retry_of 关联原调用，不重复创建研究任务或重复通知下游

### Requirement: AI 草稿与业务事实分离
系统 SHALL 持久化 Dify 原始响应与结构化草稿，并保存逐步校验状态（Schema→引用存在性→引用一致性→时间可见性→授权用途→实体身份→来源冲突）；校验未全部通过的草稿 MUST 保留原始响应与失败原因但 MUST NOT 升级为事实、研究事件或候选输入；AI 置信度 MUST NOT 替代来源核验。草稿表 MUST NOT 被用作事实主表查询。

#### Scenario: Schema 校验失败
- **WHEN** Dify 返回的 JSON 不符合约定 Schema
- **THEN** 草稿标记 `SCHEMA_INVALID` 并保存原响应，不自动盲目重试，不进入研究事件

#### Scenario: 引用不支持主张
- **WHEN** 草稿中的事实候选引用无法在材料中定位或语义不支持主张
- **THEN** 草稿标记 `CLAIM_UNVERIFIED`，相关主张保持待核验，阻断候选升级

### Requirement: 研究事件仅由 Spring Boot 写入并版本化
系统 SHALL 持久化核验后的研究事件，含事件类型、影响层级、影响范围、核验后方向、状态机（NEW→COLLECTED→DEDUPLICATED→AI_ANALYZED→VERIFIED→VARIABLE_MAPPED→PUBLISHED→DISMISSED/EXPIRED/REVISED 及异常态）、来源草稿引用、版本链与发布事件关联；Dify MUST NOT 直接写入或修改研究事件；重大新闻 MUST 只产生研究事件、风险复核任务或人工确认任务，MUST NOT 直接产生订单副作用。

#### Scenario: 核验通过发布
- **WHEN** 草稿通过全部校验且人工确认（如需要）完成
- **THEN** 研究事件状态推进至 PUBLISHED，发布事件携带幂等键与关联标识，重复发布不产生重复下游副作用

#### Scenario: 事件修订
- **WHEN** 已发布研究事件被更高等级来源否定
- **THEN** 生成新版本并标记 REVISED，旧版本保留，依赖该事件的研究链被标记需复核

### Requirement: 证据链可追溯
系统 SHALL 为研究事件持久化证据记录，区分证据类型（一手来源/独立二手/派生数据/AI 抽取/用户备注），包含材料引用、快照引用、原文引文与定位、内容哈希、发布/可见时间与核验状态；用户备注与 AI 内容 MUST NOT 伪装为外部来源证据。

#### Scenario: 从结论回源
- **WHEN** 用户或审计从研究事件结论查看依据
- **THEN** 每条证据可定位到材料版本、原文片段与来源时间，AI 抽取类证据带有明确的未确认标记

### Requirement: Dify 不可用安全降级
系统 SHALL 在 Dify 不可用或超时时保持原始采集、结构化指标、既有研究事件与确定性链路正常运行；新材料 MUST 标记为待 AI 分析状态，MUST NOT 伪造摘要、事实或方向结论，MUST NOT 将待分析材料升级为可开仓研究结论。

#### Scenario: Dify 服务中断
- **WHEN** Dify 连续调用失败触发降级
- **THEN** 采集与质量治理任务继续，新研究任务排队或标记 `AI_ANALYSIS_PENDING`，调用记录保存失败原因，恢复后按幂等键补偿
