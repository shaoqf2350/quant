# Spec Delta

## Purpose

定义数据与标的基础能力在 Spring Boot 代码仓中的可验证运行行为：服务端负责数据源、快照、质量、点时、标的、日历、行情和研究任务的确定性处理，并与既有 Paper/风控链路保持边界隔离。

## ADDED Requirements

### Requirement: 数据基础模块按职责分层
代码仓 SHALL 将数据基础能力分为共享类型、领域对象/端口、市场数据适配与治理、研究编排、任务入口和 Web 门面；依赖方向 MUST 从 Web/任务指向应用服务，再指向领域端口，禁止 Controller、Dify 适配器或调度 Handler 直接写数据库。

#### Scenario: 采集请求进入系统
- **WHEN** 管理员发起数据源测试或采集请求
- **THEN** 请求经过认证/权限、应用服务、领域校验和 Repository 事务，Controller 不直接调用 Mapper 写表

#### Scenario: 模块依赖检查
- **WHEN** 构建 `quantpf-marketdata`、`quantpf-research` 和 `quantpf-job`
- **THEN** 依赖图不允许领域模块反向依赖 Web/Dify/调度实现，循环依赖使构建失败

### Requirement: 代码使用统一 ID 与日志字段
所有新增 MySQL 业务实体 SHALL 使用应用生成的 `BIGINT` 主键，不得依赖自增；MyBatis-Plus 主键使用统一 `IdentifierGenerator`，业务侧手动发号使用共享 `workerId/datacenterId` 的 Hutool Snowflake；持久化写入 SHALL 自动填充 `create_by/create_time/update_by/update_time/remark`，且不覆盖显式审计操作者。

#### Scenario: 新增数据源实体
- **WHEN** 服务创建一个数据源记录
- **THEN** 记录在 INSERT 前获得非空雪花 ID，数据库不生成 ID，并写入创建人/创建时间/备注五个末尾日志字段

#### Scenario: 系统任务更新质量问题
- **WHEN** XXL-JOB 任务自动关闭一个已解决的数据质量问题
- **THEN** `update_by` 使用约定系统身份或 NULL，`update_time` 更新，原始创建人和创建时间保持不变

### Requirement: 数据源与快照服务可回放
Spring Boot SHALL 提供数据源目录、主备来源、采集任务、原始快照、质量问题、点时版本和用途资格的应用服务；每次写操作 MUST 幂等、可审计，并能从结构化记录回溯到原始快照和解析版本。

#### Scenario: 重复采集同一内容
- **WHEN** 同一来源以相同内容哈希重复采集
- **THEN** 服务返回既有快照业务记录并更新观测，不重复创建研究材料或下游事实

#### Scenario: 点时查询
- **WHEN** 验证服务按决策时间读取数据
- **THEN** 应用只返回 `available_at` 不晚于决策时间且用途资格允许的版本，并返回 snapshot/version 引用

### Requirement: 标的与交易日历服务提供确定性资格
代码 SHALL 将标的身份、版本化交易属性、期货品种、日历、时段、标的绑定、数据就绪和公司/合约事件封装为确定性查询服务；Dify、前端和下游策略不得自行推导最终可交易资格。

#### Scenario: 日线未就绪
- **WHEN** 交易日已开放但日线尚未接收或校验失败
- **THEN** 查询返回“等待数据/暂不生成模拟建议”，`strategy_ready` 和 `paper_ready` 为 false，且新策略触发与 Paper 新增风险被阻断

#### Scenario: 连续合约被用作执行对象
- **WHEN** 下游请求使用连续合约 ID 创建执行计划
- **THEN** 标的资格服务拒绝该请求并返回“仅可分析/筛选，不能执行”的结构化原因

### Requirement: DuckDB 行情适配与 MySQL 业务隔离
代码 SHALL 通过独立的 DuckDB adapter 管理 K 线/Tick schema 初始化、版本校验、批量导入和自然键幂等；盘口摘要、质量、特征、研究和业务状态仍经 MySQL Repository 访问。DuckDB 适配器不得依赖 Paper 订单或真实 Broker。

#### Scenario: 容器重启后恢复行情
- **WHEN** Spring Boot 重启且 DuckDB 文件位于持久化卷
- **THEN** schema initializer 校验版本和 checksum，不重复建表，历史 K 线/Tick 仍可查询

#### Scenario: 非法行情写入
- **WHEN** K 线 high 小于 low 或交易日/标的身份无效
- **THEN** 适配器拒绝写入并提交数据质量问题，不能产生有效行情行或 Paper 新开仓资格

### Requirement: 数据基础模块不得越权进入交易链
模块一应用服务 SHALL 只能发布数据健康、资格、标的、行情和研究任务相关结果，不得创建/取消订单、修改资金/持仓/风险状态或调用 QMT/CTP/真实交易接口；既有 Paper 风控与事务能力必须保持可独立运行。

#### Scenario: 数据异常影响 Paper
- **WHEN** 行情过期或授权失效被检测到
- **THEN** 模块一发布数据阻断/资格变化事件，由模块四决定 Paper 风险处置；模块一不直接平仓、撤单或修改账户

#### Scenario: Dify 不可用
- **WHEN** 研究解释服务不可用
- **THEN** 采集、质量、标的、日历、确定性行情和既有 Paper/风控查询继续运行，新 AI 研究任务进入待处理或降级状态
