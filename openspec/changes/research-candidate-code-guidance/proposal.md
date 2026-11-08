# Proposal

## Why

模块二【研究与候选发现】的数据库详细设计（`research-candidate-db-design`）已经冻结了事实核验、研究链、资产研究因子、候选筛选/评分、研究卡、审核、交接共 13 张表的持久化边界；但代码仓 `quantpf-research` 仍只有模块一 `data-foundation-code-guidance` 规划的研究入口骨架（任务/草稿/事件），`quantpf-domain` 也没有 claim/thesis/candidate/card/handoff 等模块二领域对象，`quantpf-strategy` 是空骨架。没有与 Java 21 工程及已冻结数据库设计相匹配的代码落地指导，后续施工容易把确定性因子/评分交给 Dify、把研究分误当总分、让研究域越权写策略/订单，或重复造 ID/审计能力。本 change 补上模块二的分层、关键类职责、研究型 Dify 集成与配置契约，使模块二可测试、可回放、可幂等，并与模块一、模块三/四边界清晰。

## What Changes

- 生成与已完成数据库设计对应的代码仓库落地指导文档（`design.md`）：
  - 明确 `quantpf-domain`、`quantpf-research`、`quantpf-strategy`、`quantpf-job`、`ruoyi-admin` 在模块二范围的分层目录结构与依赖方向，并声明复用模块一已有的 Dify 适配/校验/ID/日志字段/错误码基础；
  - 设计事实核验、研究链、资产研究、候选筛选/评分、研究卡、审核、交接七类应用服务的 Domain/Application/Persistence/Adapter/Controller 职责与禁止项；
  - 明确关键类/接口（`ClaimVerificationService`、`ResearchChainService`、`FundamentalFactorCalculator`、`EventExposureMapper`、`CommodityResearchService`、`ScreeningRunner`、`CandidateAssessor`、`ResearchCardService`、`ResearchReviewService`、`CandidateHandoffService`、`ResearchQaService` 等）职责与依赖方向；
  - 承接模块一雪花主键 + 五个末尾日志字段 + 显式事务 + 幂等/乐观锁约定，落到 13 张表的服务层；明确研究评分 70 分制与总 100 分门槛的边界；
  - 设计模块二研究型 Dify Workflow（影响分析、股票/商品/量价研究解释、研究卡说明、问答、复盘摘要）的调用约定、确定性计算与 Dify 草稿的分离、审批/降级/限额；
  - 明确 `application.yml`/环境变量需新增的配置项（评分、筛选、研究问答限额、研究卡解释等），敏感配置不落库、不进日志、不进前端。
- 新增行为规格，场景采用可验证 WHEN/THEN，覆盖：
  - 事实核验/研究链/候选筛选/研究卡/审核/交接的服务端状态迁移与幂等；
  - 确定性因子与评分由 Spring Boot 计算、Dify 不得修改；
  - 研究型 Workflow 输入最小上下文、输出校验、降级；
  - 管理员、审核用户、引导型普通用户从研究任务到候选/研究卡/审核/交接的关键操作路径与 Paper/策略前置阻断。
- 本 change 只做代码仓指导与行为契约，不实现 Java 代码、DDL、Dify Workflow 或前端；实现由后续 apply 阶段执行。

## Capabilities

### New Capabilities

- `research-candidate-runtime`: 研究与候选发现模块在 Spring Boot 工程的分层运行能力——事实主张核验、研究假设/预测/反方、资产研究因子、候选筛选与评分、研究卡版本、人工审核、候选交接的服务端行为、事务/幂等/乐观锁/硬门禁与 ID/日志字段约定。
- `research-dify-workflows`: 模块二研究型 Dify Workflow 的受控集成能力——影响分析、资产研究解释、研究卡说明、问答与复盘摘要的最小上下文、输出校验、确定性计算与 Dify 草稿分离、降级与权限边界。
- `research-candidate-user-journeys`: 面向管理员、审核用户与引导型普通用户的从研究任务到事实核验、候选/研究卡、审核与模块三交接的可观察操作路径及可验证状态反馈。

### Modified Capabilities

（无——主 `openspec/specs/` 当前为空，本 change 全部为新增能力。模块一 `dify-research-integration` 是研究入口的基础集成契约，本 change 只在其上扩展研究型 Workflow，不修改其定义。）

## Impact

- **代码模块**：`RuoYi-Vue-Plus/quantpf-modules/quantpf-domain`（新增 research/asset 领域对象与 Repository 端口）、`quantpf-research`（新增 claim/thesis/screening/card/handoff/asset 应用服务与 Dify 研究型适配）、`quantpf-job`（筛选/复核 Handler）、`quantpf-strategy`（只暴露交接端口），以及 `ruoyi-admin` 的 REST/回调入口。
- **配置**：`ruoyi-admin/src/main/resources/application*.yml`、环境变量，新增研究评分、筛选、研究问答限额、研究卡解释等配置项；沿用模块一 `quantpf.id.*`、`quantpf.dify.*`。
- **依赖**：现有 Java 21、Spring Boot、MyBatis-Plus、RuoYi Sa-Token、Flyway/MySQL；复用模块一已规划的 Dify WebClient 适配与校验服务；不引入第二套调度，不新增真实交易/策略实现。
- **契约**：研究主张、研究链、候选、研究卡、审核、交接、研究问答相关 OpenAPI、事件 Schema、错误码与菜单权限。
- **安全边界**：Dify 只接收经授权的最小研究上下文并产草稿；不接触 MySQL/DuckDB/Redis/Paper 凭据，不写事实、评分、研究卡、交接或订单；一期不产生 LIVE/QMT/CTP 路径。
- **现有能力保护**：复用模块一研究入口的服务骨架与 `quantpf-domain` Paper 事务/审计；模块二不得直接写 Paper 订单、不得绕过模块三策略与模块四 RiskCheck。