# Tasks

> 本清单是后续实施计划，不代表源码已修改。按用户指定顺序组织：数据库 → 后端实体 → 后端接口 → Dify 集成 → 前端页面 → 前端接口。TradingAgents 以 Dify 按需调用 Adapter 为标准路径；不为浏览器开放 TA 地址，不允许 Dify/TA 直连 QuantPF 数据库或交易写接口。
>
> 路径约定：`[后端]` = `D:\workspace\quant\RuoYi-Vue-Plus`；`[前端]` = `D:\workspace\quant\plus-ui`。后端仓库使用 Flyway，数据库版本迁移应放在 `[后端]/db/migration/`，而非模板中的 `[后端]/sql/`；`[后端]/script/sql/` 仅按仓库现有约定用于种子或运维脚本。DuckDB schema/迁移按设计中的专用资源目录与启动校验实现，不得将 MySQL DDL 当作 DuckDB DDL。

## 1. 数据库

- [ ] 1.1 盘点后端 `db/migration/` 中 V1.0–V1.9 已发布迁移、现有 QuantPF 表、索引/外键/ID 生成规则及 Paper 依赖，形成表级差异清单；通过迁移目录审查、测试库 `information_schema` 对比和 Paper 回归基线确认无历史迁移改写。
- [ ] 1.2 在 `[后端]/db/migration/` 新增向前兼容的 MySQL Flyway 迁移，创建数据源/授权/采集基础表：`quantpf_vendor`、`quantpf_data_product`、`quantpf_data_interface`、`quantpf_data_field`、`quantpf_data_source`、`quantpf_source_group`、`quantpf_source_group_member`、`quantpf_collection_task`、`quantpf_collection_attempt`、`quantpf_data_raw_snapshot`、`quantpf_data_observation`；逐表落实 `design.md` 的字段类型、NOT NULL/默认值、应用生成 BIGINT 主键、唯一键/查询索引、审计列与保留策略；以 Flyway 空库迁移、重复迁移和表/索引断言测试验证。
- [ ] 1.3 新增质量、点时和用途资格表迁移：`quantpf_data_quality_issue`、`quantpf_data_point_in_time`、`quantpf_data_use_eligibility`；落实来源/快照/有效时间关联、状态约束、可追溯索引与禁止覆盖历史的规则；以授权拒绝、质量问题检索和 as-of 查询集成测试验证。
- [ ] 1.4 新增标的、日历与生命周期表迁移：`quantpf_future_product`、`quantpf_instrument_version`、`quantpf_instrument_relation`、`quantpf_trading_calendar_def`、`quantpf_trading_calendar_day`、`quantpf_trading_session_template`、`quantpf_instrument_calendar_binding`、`quantpf_calendar_source_snapshot`、`quantpf_calendar_conflict`、`quantpf_fx_session_rule`、`quantpf_corporate_action`、`quantpf_contract_event`；按设计实现版本唯一性、日历冲突追踪及业务时区字段；以标的身份、交易日/时段和冲突回放迁移测试验证。
- [ ] 1.5 新增行情准备和分析摘要表：`quantpf_market_data_readiness`、`quantpf_market_quote_snapshot`、`quantpf_market_state`、`quantpf_capacity_assessment`、`quantpf_quant_feature_snapshot`，以及预留商品/现货表 `quantpf_spot_quote`、`quantpf_supply_demand_observation`、`quantpf_basis_term_structure`；明确 MySQL 元数据与 DuckDB 高容量行情数据的分工、快照版本及容量资格索引；以 MySQL/DuckDB schema 检查、数据抽样对拍和准备度查询测试验证。
- [ ] 1.6 新增研究材料与 AI 运行表：`quantpf_research_material`、`quantpf_research_material_relation`、`quantpf_research_task`、`quantpf_research_task_run`、`quantpf_dify_workflow_registry`、`quantpf_dify_invocation`、`quantpf_trading_agents_run`、`quantpf_trading_agents_agent_view`、`quantpf_ai_research_draft`、`quantpf_ai_validation_result`、`quantpf_research_event`、`quantpf_event_evidence`、`quantpf_schema_version`、`quantpf_schema_marker`；落实幂等键、运行关联、角色级 PARTIAL 状态、版本化校验结果、AI_DRAFT 隔离及大对象 MinIO 引用；以重复提交、部分智能体失败、草稿不可直接发布和引用可追溯的数据库集成测试验证。
- [ ] 1.7 对既有 `quantpf_instrument`、`quantpf_trading_calendar`、`quantpf_market_bar`、`quantpf_research_news`、`quantpf_financial_statement`、`quantpf_audit_record` 制定独立兼容迁移；新增字段/索引后回填并对拍，旧表按设计冻结或保留兼容读，不改历史 Flyway 文件；以迁移前后行数/Hash 对账、回滚演练及 Paper 冒烟测试验证。
- [ ] 1.8 为 DuckDB 新增独立 schema 初始化/版本迁移和单写入访问约定，为 MinIO 配置受控 Bucket/对象键元数据与保留策略；MySQL 只保存对象引用、Hash、大小和版本，不保存大正文；以 DuckDB 并发写保护测试、MinIO 权限/引用测试及备份恢复演练验证。
- [ ] 1.9 同步更新 `[后端]/contract/` 中的数据库基线/迁移说明及模块一数据字典，列明所有现有、演进、新增表与 Dify/TradingAgents 的只读输入和 AI 草稿输出关系；通过文档表清单与 Flyway/DuckDB 实际 schema 自动核对确认一致。

## 2. 后端实体

- [ ] 2.1 在 `[后端]/quantpf-modules/quantpf-domain/` 按 data-source、collection、quality、instrument、calendar、marketdata、research、ai 子域建立领域实体/值对象/枚举；表字段与 `design.md` 字典一致，ID/时间/状态类型符合若依与 MySQL 约定；通过编译、实体映射测试和枚举序列化测试验证。
- [ ] 2.2 为数据源、来源组、采集快照/观测、质量问题、点时和用途资格补齐 Mapper/Repository 接口及基础设施实现（按仓库实际持久层规范）；查询限定组织/权限范围并支持版本和 as-of 条件；以 Repository 单元测试和越权数据隔离测试验证。
- [ ] 2.3 为标的版本/关系、日历定义/日期/时段/冲突、生命周期事件、行情准备/摘要实体补齐 Repository 与领域规则；旧表兼容实体不得与新权威模型混淆；以标的身份唯一性、交易时段解析和旧数据兼容测试验证。
- [ ] 2.4 为研究任务、任务运行、Dify 调用、TradingAgents 运行/逐角色观点、AI 草稿、验证结果、研究事件/证据建立实体及 Repository；正文、密钥和 AI 原文按设计只保存受控引用/脱敏结构；以幂等唯一键、部分成功保留、草稿状态不可越级测试验证。
- [ ] 2.5 实现 DuckDB 行情读取/写入边界对象及 MinIO 对象引用对象，禁止领域实体携带数据库凭据或暴露可执行的公开对象 URL；以边界架构测试和序列化快照测试验证。
- [ ] 2.6 补齐实体、Repository 和状态机文档（含字段到迁移版本映射、MySQL/DuckDB/MinIO 归属）；通过模块编译、单元测试与文档映射校验确认本阶段交付完整。

## 3. 后端接口

- [ ] 3.1 在 `[后端]/quantpf-modules/quantpf-marketdata/` 和 `quantpf-research/` 实现应用服务/用例层，分别负责确定性采集治理、标的日历行情资格、研究任务创建与上下文快照；创建任务先做鉴权、用途授权、点时和幂等检查，未经允许不得进入 Dify；以应用服务单测验证拒绝路径与成功路径。
- [ ] 3.2 在 `[后端]/ruoyi-admin/` 增加模块一 REST Controller（数据源、质量、标的、日历、行情准备、研究任务、草稿审核、研究事件/证据），统一使用若依鉴权、参数校验、分页、审计与错误码；通过 MockMvc/API 契约测试验证权限、HTTP 状态、响应 Schema 和无浏览器直连 AI 服务。
- [ ] 3.3 定义并落地研究任务状态机、取消/重试、运行查询、AI 草稿审核/驳回/发布及证据校验 API；普通创建接口不得直接写入权威研究事件；以状态转换表驱动测试验证所有允许/禁止转换。
- [ ] 3.4 在 `[后端]/contract/openapi/`、`contract/events/`、`contract/error-codes/` 和 `contract/menu/quantpf-menu-facts.yaml` 登记 API、统一状态事件、错误码、权限码/菜单；为 Dify 与 Adapter 内部契约标明版本和兼容策略；运行仓库 OpenAPI/菜单/契约校验命令并确认生成物无未审差异。
- [ ] 3.5 实现 API 审计、traceId/invocationId/taskId 贯通、敏感字段脱敏、限流与组织范围校验；以日志捕获测试、越权/注入 API 测试验证无密钥、账户凭据或未经授权正文泄露。
- [ ] 3.6 为 Controller、应用服务、权限和错误码编写接口使用文档及自动化测试；运行后端相关模块编译、单测、集成测试和既有 Paper/Risk 回归确认无接口阶段回归。

## 4. Dify 集成（含 TradingAgents 按需接入）

- [ ] 4.1 在后端建立 Dify Workflow 注册与版本白名单、输入上下文组装器、Dify Client/SSE 事件适配器；Spring Boot 使用服务端密钥调用 `/v1/workflows/run`，浏览器不接触 Dify；以 WireMock/Stub 覆盖正常事件、节点失败、断流恢复和重复事件，验证同一幂等键只产生一个运行/草稿。
- [ ] 4.2 建立 TradingAgents Adapter Client 与版本化 REST/JSON 契约（创建运行、查询运行、取消、健康状态）；标准调用由 Dify Workflow 发起，Spring Boot 仅运维/健康/取消或经显式批准的降级路径可调用同一 Adapter；以 Adapter Stub 契约测试验证未授权 URL、MCP/脚本/数据库直连均不可作为生产主路径。
- [ ] 4.3 在 Dify Workflow 中实现任务路由：简单抽取/摘要/格式转换不触发 TA；复杂影响链、多空辩论、重要材料反方研究按策略触发 TA；保留角色输出、冲突和缺失角色并生成结构化 AI_DRAFT；以 Dify 测试环境和 TA Stub 验证两种分支及 PARTIAL 结果不被标为完整辩论。
- [ ] 4.4 实现上下文最小化、授权/点时/来源/标的身份校验、提示注入隔离、Schema/引用校验与双层验证结果持久化；校验失败时不升级权威事实；以未授权材料、越界引用、错误 Schema、敏感字段和交易指令注入测试验证均阻断或安全降级。
- [ ] 4.5 实现运行幂等、有限重试/轮询、超时、熔断、并发/Token/成本限制、Dify/TA 不可用降级、MinIO 大对象存储、密钥按环境隔离与日志掩码；以故障注入、安全扫描和密钥泄露测试验证安全默认值与可恢复性。
- [ ] 4.6 在 `[后端]/src/main/resources/` 及部署样例中登记 Dify、TradingAgents、回调、MinIO、限额等配置项及 dev/test/prod 环境变量示例，默认关闭未配置的 AI 功能；验证启动配置检查、缺少密钥时拒绝匿名调用且仓库不含真实凭据。
- [ ] 4.7 编写 Dify Workflow/Adapter 运维、版本发布、Stub 联调、告警和故障处置文档；运行 Dify→TA Adapter→Dify→Spring Boot 端到端测试，确认全过程只生成 AI_DRAFT，未经审核不改变权威事实或订单。

## 5. 前端页面

- [ ] 5.1 在 `[前端]/src/views/quant/` 建立模块路由和页面骨架：数据源与来源组、数据质量、标的中心、交易日历、行情准备、研究任务、AI 草稿审核、研究事件/证据；按角色/权限配置路由与菜单；以路由/菜单自动化测试验证无权限页面不可见且深链访问受拦截。
- [ ] 5.2 实现数据源/来源组管理、连通性测试、授权期限提示、采集任务与尝试详情、质量问题定位/处置页面；表单不得展示密钥明文，写操作有确认和审计提示；以组件测试验证权限、掩码、表单校验和失败提示。
- [ ] 5.3 实现标的身份/版本、交易日历来源冲突、行情准备度/容量评估页面，清晰标注确定性数据状态与数据时间；操作不得由 AI 自动修改标的/日历/资格；以页面测试验证冲突可见和人工确认路径。
- [ ] 5.4 实现研究任务创建与详情页：从新闻/材料/标的入口触发“Dify 研究/深度多智能体辩论”，提交时展示授权、时间点、材料和研究模式；流式显示任务状态、当前阶段、已完成/待处理角色，允许取消/稍后查看；以端到端测试验证简单任务不误报 TA、深度任务状态正确且浏览器请求仅指向 Spring Boot。
- [ ] 5.5 实现 AI 草稿/辩论审核页，展示 AI_DRAFT 标识、Dify/Workflow/TA运行版本、逐角色观点、冲突、缺失角色、引用证据、校验失败原因和数据时间；仅有审核权限时开放审核/驳回/发布操作，无下单入口；以组件/权限测试验证不可越权发布且 PARTIAL 明确提示。
- [ ] 5.6 实现加载、空态、超时、断线重连、Dify 不可用、TA 熔断、单模型降级和权限不足文案；以视觉回归和状态组件测试确认不将降级结果呈现为多智能体完整结论。
- [ ] 5.7 更新页面操作说明、权限矩阵和菜单种子/生成说明；运行前端 lint、类型检查、组件测试及关键路径 E2E，确认 UI 阶段交付可验收。

## 6. 前端接口

- [ ] 6.1 在 `[前端]/src/api/quant/` 创建数据源、采集、质量、标的、日历和行情准备 API 封装及 TS 请求/响应类型；仅调用 Spring Boot QuantPF API，统一错误处理/分页/取消令牌；以 API 单测验证 URL、参数和类型约束。
- [ ] 6.2 创建研究任务、运行状态/SSE、取消/重试、AI 草稿/审核、研究事件/证据 API 封装与 TS 类型；禁止把 Dify App Key、TA Token 或 Adapter URL 写入前端配置；以静态扫描和 API Mock 测试验证前端产物无服务端凭据/内部端点。
- [ ] 6.3 统一字典枚举、状态标签、权限指令与错误码映射（包括 AI_DRAFT、PARTIAL、RETRYABLE_FAILED、AUTHORIZATION_BLOCKED 等）；以类型检查和状态映射单测验证未知值安全显示且不可默认当作成功。
- [ ] 6.4 为前端 API 编写调用约定和页面到接口映射文档；运行 TypeScript 类型检查、lint、API 单测及前后端 OpenAPI 兼容校验，确认接口变更可追踪。

## 7. 集成验收（跨阶段）

- [ ] 7.1 在测试环境从空库执行 MySQL Flyway 和 DuckDB schema 初始化，并完成既有数据迁移对拍、MinIO 对象引用校验和恢复演练；验收报告记录行数/Hash、索引、备份恢复结果。
- [ ] 7.2 运行模块一全链路场景：授权材料进入 Spring Boot 快照 → Dify 简单流程或按需调用 TradingAgents → Dify 汇总 → Spring Boot 校验 → 人工审核 → 事件发布；验证 taskId、traceId、invocationId、difyRunId、tradingAgentsRunId 和证据可关联。
- [ ] 7.3 运行安全与故障注入回归：越权/未授权、Dify/TA 超时、SSE 断线、Adapter PARTIAL、回调伪造/重放、密钥日志泄露、交易指令注入、Paper 写端点访问；确认 AI 不可用不影响确定性数据链路，且 Paper 订单/持仓/风控状态无变化。
- [ ] 7.4 汇总旧模块一四个在途 change 与《详设_0》《详设_1》《详设_6》差异，更新权威索引/文档映射并取得模块一架构评审通过；验收表、迁移回滚方案、运行手册和未决环境参数均有责任人与结论。
- [ ] 7.5 执行后端 Maven 构建/测试、前端 lint/typecheck/test/build、OpenSpec 校验及 Paper 回归；只有所有必需检查通过、AI Feature Flag 默认关闭且灰度/回滚步骤通过评审，方可申请生产启用。

## Workflow follow-up

- 评审并确认本提案后，使用 OpenSpec apply 工作流按上述六个实施阶段推进；本提案不授权直接修改源码。
- 生产 Dify Workflow、模型版本、TradingAgents Adapter 地址/认证、对象保留期限与历史数据迁移窗口由部署环境在实施前登记并经安全/数据授权评审批准。
