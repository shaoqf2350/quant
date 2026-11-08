# Proposal

## Why

数据库详细设计已经冻结了模块一的数据治理、标的/日历、行情、研究入口和 Dify 持久化边界，但代码仓目前只有 `quantpf-domain` 的 Paper 基础能力，`quantpf-common`、`quantpf-marketdata`、`quantpf-research` 仍是骨架，且现有 Spring 装配明确没有 Redis、调度、Broker 或 Dify Bean。缺少与实际 RuoYi-Vue-Plus/Java 21 工程相匹配的施工指导，后续实现容易越权写库、重复造 ID/审计能力、把 Dify 当事实源，或把现有 Paper 交易链路与数据采集耦合。

## What Changes

- 生成与已完成数据库设计对应的代码仓库落地指导文档（`design.md`）：
  - 明确 `quantpf-common`、`quantpf-domain`、`quantpf-marketdata`、`quantpf-research`、`quantpf-job`、`ruoyi-admin` 的目录结构、依赖方向和禁止反向依赖；
  - 设计数据源/快照/质量/点时/标的/日历/行情/研究任务/AI 草稿/研究事件的 Domain、Application、Persistence、Adapter、Controller 分层；
  - 明确关键类、接口、端口、Repository、Mapper、事务边界、事件和错误处理职责；
  - 对接既有 MyBatis-Plus `ASSIGN_ID`/`IdentifierGenerator` 与 Hutool Snowflake 的共享 `workerId/datacenterId` 配置，落实五个末尾日志字段的自动填充；
  - 设计 DuckDB JDBC 初始化、schema 版本、K 线/Tick 读写适配与 MySQL 业务主库隔离；
  - 设计 Dify v1.16.1 Workflow API、Bearer Key、流式响应消费、幂等、超时、重试、Schema 校验、引用校验和可选签名回调；
  - 明确 `application.yml`/环境变量/配置属性需要新增的配置项，敏感配置不落库、不进日志、不进前端。
- 新增行为规格，场景全部采用可验证的 WHEN/THEN 格式，覆盖：
  - 管理员配置数据源并查看授权/健康状态；
  - 日历/标的同步与数据就绪阻断；
  - 原始快照去重、质量问题处理和点时查询；
  - K 线/Tick 入库与 DuckDB 重启恢复；
  - 普通用户查看“可研究/等待数据/暂不生成模拟建议”；
  - 用户创建研究任务、Dify 草稿校验、研究事件发布；
  - Dify 超时/不可用降级；
  - Dify 不得直连数据库、不得写事实/订单/风险；
  - 既有 Paper 风控链路不被数据模块绕过。
- 新增实施任务清单，按依赖顺序覆盖模块骨架、配置、数据访问、领域服务、Dify 适配、API/事件、测试和文档同步。
- 本 change 只做代码仓指导和行为契约，不实现 Java 代码、Flyway、DuckDB DDL、Dify Workflow 或前端页面；实现由后续 apply 阶段执行。

## Capabilities

### New Capabilities

- `data-foundation-runtime`: 数据与标的基础模块在 Spring Boot 工程中的分层运行能力，包括数据源/快照/质量/点时/标的/日历/行情/研究任务的服务端行为、配置、ID/日志字段填充和 DuckDB 适配边界。
- `dify-research-integration`: Spring Boot 与自托管 Dify Workflow 的受控集成能力，包括最小上下文、流式响应、可选签名回调、Schema/引用/时间/授权校验、幂等重试和安全降级。
- `data-foundation-user-journeys`: 面向管理员、审核用户和引导型普通用户的数据可用性、研究任务、Dify 草稿、研究事件和 Paper 前置状态的用户操作行为及可验证状态反馈。

### Modified Capabilities

（无——当前 `openspec/specs/` 为空，本 change 新增代码仓行为能力。）

## Impact

- **代码模块**：`RuoYi-Vue-Plus/quantpf-modules/quantpf-common`、`quantpf-domain`、`quantpf-marketdata`、`quantpf-research`、`quantpf-job`，以及 `ruoyi-admin` 的配置和 REST/回调入口。
- **配置**：`ruoyi-admin/src/main/resources/application*.yml`、环境变量、Dify 服务地址/应用 Key、DuckDB 路径/卷、ID 生成器和数据源/超时/重试参数。
- **依赖**：现有 Java 21、Spring Boot、MyBatis-Plus、RuoYi Sa-Token、Redis/Redisson、Flyway/MySQL；按需引入 DuckDB JDBC 和 WebClient 能力，不能恢复/新增第二套调度框架。
- **契约**：数据源、日历、研究任务、Dify 草稿、研究事件、质量问题相关 OpenAPI、事件 Schema、错误码和菜单权限。
- **安全边界**：Dify 只接收经授权的最小研究上下文；不接触 MySQL/DuckDB/Redis/Paper 凭据，不直接修改事实、风险、账户、订单、持仓或交易状态；一期不产生 LIVE/QMT/CTP 路径。
- **现有能力保护**：复用现有 `quantpf-domain` Paper 事务、幂等、审计和风险闸门；模块一不得直接写 Paper 订单或绕过模块四 RiskCheck。
