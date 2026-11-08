# Spec Delta

## Purpose

定义模块二研究型 Dify Workflow（事件影响分析、股票/商品/量价研究解释、研究卡说明、问答、复盘摘要）的受控集成行为：确定性因子/评分/筛选由 Spring Boot 计算，Dify 只生成解释与研究草稿，且只能访问最小授权上下文。

## ADDED Requirements

### Requirement: 研究型 Workflow 只调用已发布版本与最小上下文
系统 SHALL 仅调用已登记、已发布且环境匹配的研究型 Workflow 版本，请求只包含研究任务 ID、材料/来源/标的引用、点时快照、授权/质量状态与 Workflow/Prompt/Schema 版本，不得包含交易凭据、账户明细或无关个人信息。

#### Scenario: 调用未发布的研究型 Workflow
- **WHEN** 研究任务指定的 Workflow 版本未发布或环境不匹配
- **THEN** Spring Boot 拒绝调用并记录审计错误，不产生 Dify run 或候选拔高

#### Scenario: 资产研究解释请求
- **WHEN** 股票/商品研究需要 Dify 生成解释草稿
- **THEN** 请求只包含已核验事实与结构化指标引用，凭据由服务端注入且不出现在日志或前端

### Requirement: 确定性研究计算不交给 Dify
系统 SHALL 由 Spring Boot 确定性计算基本面因子、事件暴露/敏感度、候选硬门禁、分项评分与筛选；Dify 只能解释计算结果，不得计算、修改或补造因子值、评分、方向、机会状态或预测统计。

#### Scenario: Dify 返回自算评分
- **WHEN** Dify 响应中包含其自行计算的评分、概率或因子值
- **THEN** 系统忽略或标注为草稿字段，不写入候选评分、因子快照或研究卡统计值

#### Scenario: 研究卡说明只读
- **WHEN** Dify 生成研究卡普通语言说明
- **THEN** 说明基于已审定的结构化研究卡，Dify 不得修改研究卡的状态、方向、评分或版本

### Requirement: Dify 输出按事实/解释/未知分层校验
系统 SHALL 校验研究型 Workflow 输出的事实候选、解释草稿、预测草稿与未知项，并区分"事实、系统计算、研究解释、AI 草稿、未知"；引用不可定位或未经核验的草稿不得进入候选评分或研究卡。

#### Scenario: 解释草稿缺引用
- **WHEN** Dify 的影响分析草稿引用的材料或结构化指标不存在或不能定位
- **THEN** 系统标记该解释待核验或拒绝，不纳入候选方向或研究卡证据

#### Scenario: 草稿全通过
- **WHEN** Schema、引用、时间、授权、身份与冲突校验全部通过
- **THEN** 系统保存为可研究草稿，由 Spring Boot/人工流程决定是否升级为核验主张或研究假设，Dify 不能直写主表

### Requirement: 研究型 Workflow 降级不影响确定性研究
系统 SHALL 在 Dify 不可用、限流或持续超时安全降级，停止无界重试并保留补偿任务；确定性因子计算、候选筛选、既有研究卡与交接继续运行，新 AI 解释任务进入排队/待分析，不得伪造解释或研究结论。

#### Scenario: Dify 连续不可用
- **WHEN** 研究型 Workflow 达到降级阈值
- **THEN** 系统标记 Dify 降级并保留补偿任务，仍能运行确定性筛选与展示既有研究卡，标记新解释为待补充

### Requirement: Dify 权限与网络边界
系统 SHALL 通过服务端适配器访问 Dify，禁止浏览器直连；Dify 网络身份不得访问 QuantPF 数据库、缓存、Paper、账户或风险服务；研究型 Workflow 的 API Key 只能从环境变量/密钥系统读取。

#### Scenario: Dify 尝试访问业务库
- **WHEN** Dify 使用其网络身份访问 QuantPF 数据库或研究主表写接口
- **THEN** 网络白名单与凭据权限拒绝访问并产生安全审计记录