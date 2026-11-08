# Tasks

## 1. 工程基线、共享配置与架构门禁

- [ ] 1.1 在 `quantpf-common` 建立 `QuantPfProperties`、WorkerNode、ActorContext、UTC/市场时区、错误码和事件元数据类型，并验证 Java 21/Maven 模块编译通过
- [ ] 1.2 实现统一雪花 ID 配置：MyBatis-Plus `IdentifierGenerator` 与 Hutool `SnowflakeFacade` 共用 `workerId/datacenterId`，增加时钟回拨、多实例重复配置和 ID 碰撞测试
- [ ] 1.3 扩展 MyBatis-Plus 元对象填充，支持 `create_by/create_time/update_by/update_time/remark` 末尾日志字段、人工操作者、系统任务和 NULL 规则；通过实体 INSERT/UPDATE 集成测试验证
- [ ] 1.4 在 `application.yml`、dev/prod 配置和环境变量模板中加入 ID、DuckDB、数据治理、Dify、研究重试和敏感配置项；通过配置绑定测试验证 Key 不会回显到日志或 API
- [ ] 1.5 增加 ArchUnit/依赖检查：Domain 不依赖 Web/Dify/XXL-JOB，Controller/Handler/Dify adapter 不直接依赖 Mapper，构建失败时能阻止违规依赖

## 2. Domain 对象、Repository 端口与 MySQL 持久化边界

- [ ] 2.1 在 `quantpf-domain` 增加数据源/来源组/采集任务/快照/质量/点时/用途资格对象及状态迁移规则；通过领域状态单元测试验证非法迁移被拒绝
- [ ] 2.2 增加标的/版本/期货品种/关系/日历/时段/就绪/冲突/公司行为/合约事件对象及 Repository 端口；通过连续合约执行拒绝、日历冲突和版本并发测试验证
- [ ] 2.3 增加行情/市场状态/容量/量价特征/研究材料/研究任务/AI 草稿/研究事件/证据对象和 Repository 端口；通过点时查询、资格矩阵和草稿与事实状态分离测试验证
- [ ] 2.4 按数据库详细设计增加 MyBatis Record、Mapper、XML 和 Repository Adapter，所有新业务 ID 使用 BIGINT、无自增，父子 FK 类型一致；通过 `SHOW CREATE TABLE`/Mapper 集成测试验证
- [ ] 2.5 建立数据域事务、幂等、expectedVersion 和审计上下文基础，确保重复写返回原结果、参数不同返回幂等冲突；通过并发重复命令集成测试验证

## 3. Marketdata 数据治理与标的日历服务

- [ ] 3.1 在 `quantpf-marketdata` 实现 `SourceAdapter` 端口及 Mock Tushare/新闻/规则适配器，支持供应商状态、主备来源和采集任务；通过未授权源只允许 Mock 的测试验证
- [ ] 3.2 实现 `CollectionApplicationService`、`RawSnapshotService` 和 `DataQualityService`，完成原始响应清洗、哈希去重、对象存储引用、质量问题和点时资格；通过重复快照、敏感字段清洗和 available_at 过滤测试验证
- [ ] 3.3 实现 `InstrumentApplicationService`、`TradingCalendarService` 和事件服务，覆盖标的身份、具体/连续合约、交易时段、GFEX 人工日历、日线就绪和公司/合约事件；通过 A 股/期货夜盘/GFEX/事件生效集成测试验证
- [ ] 3.4 实现 `MarketDataApplicationService` 的 MySQL 盘口摘要、市场状态、容量和量价特征边界；通过 PARTIAL/DELAYED 字段降级、UNKNOWN 容量阻断和特征版本回放测试验证
- [ ] 3.5 新增 DuckDB JDBC 配置、`DuckDbSchemaInitializer`、K 线/Tick Repository 和批量导入服务，支持 schema checksum、30m/日/周/月约束、自然键幂等和重启恢复；通过容器重启、非法 OHLC 和重复导入测试验证
- [ ] 3.6 为 marketdata 应用服务补充数据字典、OpenAPI、事件 Schema、错误码和用户可读状态映射；通过契约校验与“可研究/等待数据/暂不生成模拟建议”响应测试验证

## 4. Research 与 Dify 受控集成

- [ ] 4.1 在 `quantpf-research` 实现 `ResearchTaskApplicationService`、输入快照冻结、任务运行记录、幂等/重试/取消和补偿端口；通过重复任务、缺输入和重试恢复测试验证
- [ ] 4.2 实现 `DifyWorkflowRegistryService`、配置绑定和已发布 Workflow/环境校验；通过未发布版本、环境不匹配和敏感配置不回显测试验证
- [ ] 4.3 实现 `DifyResearchClient` 与 `DifyStreamConsumer`，调用 `/v1/workflows/run`、Bearer Key、`response_mode=streaming`，保存 invocation/run/响应 hash 并实现有限退避重试；通过 Stub Dify SSE 合同测试验证成功、中断、超时和限流
- [ ] 4.4 实现可选 `DifyCallbackVerifier`，校验时间戳、Nonce、防重放、来源白名单、HMAC-SHA256、requestId/runId 和幂等键；通过合法、非法和重复回调安全测试验证
- [ ] 4.5 实现 `AiDraftValidationService` 的 Schema、引用、时间可见性、授权、身份和冲突校验；通过非法 JSON、未知引用、未来数据、授权阻断和全部通过场景验证草稿不会越权升级事实
- [ ] 4.6 实现研究事件/证据升级与发布服务，确保只有 Spring Boot/人工审核能写研究事件，并发布可靠幂等事件；通过重复发布、事件修订和 Dify 直写拒绝测试验证
- [ ] 4.7 为 Dify 模块补充降级、限额、队列、调用状态、审计和用户可读错误文档；通过 Dify 完全不可用时采集/质量/日历/确定性行情/Paper查询继续运行的集成测试验证

## 5. Job、Web 门面与用户关键路径

- [ ] 5.1 在 `quantpf-job` 增加仅调用应用服务的 XXL-JOB Handler 端口和执行审计适配，不复制领域规则、不引入 SnailJob 双栈；通过 Handler 依赖检查和重复执行测试验证
- [ ] 5.2 在 `ruoyi-admin` 增加数据源、质量、快照、标的、日历、行情、研究任务、Dify回调、研究事件 Controller/DTO，接入 Sa-Token、资源 scope、requestId、Idempotency-Key 和用户可读错误；通过 API 权限/越权/重复请求测试验证
- [ ] 5.3 实现管理员路径：登记数据源/来源组、查看健康、提交采集/补偿和处理质量问题；通过管理员成功、普通用户拒绝、未授权源 PENDING_CONFIRMATION 场景验证
- [ ] 5.4 实现审核用户路径：查看日历/事实冲突、确认/拒绝/延期/要求补证据、检查 expectedVersion 和审计；通过过期版本拒绝和原始冲突版本保留场景验证
- [ ] 5.5 实现普通用户路径：快捷研究任务不超过三项输入、数据状态卡、AI草稿/核验事实区分、研究事件状态和 Paper 前置阻断；通过数据未就绪、Dify失败、Paper资格不足和资格满足场景验证
- [ ] 5.6 完善 OpenAPI、菜单/权限、事件 Schema、错误码、国际化文案和模块五所需状态字段；通过契约生成/校验和端到端 API 文档检查验证

## 6. 集成验收与现有 Paper 保护

- [ ] 6.1 执行完整数据链集成测试：来源→采集→原始快照→质量→点时/资格→标的/日历→DuckDB K线→研究任务→Dify草稿→研究事件/证据；通过快照、版本、幂等和事件回放验证
- [ ] 6.2 执行安全测试：浏览器不能直连 Dify、Dify 网络身份不能访问 MySQL/DuckDB/Redis/Paper/真实通道、Key/Authorization/Cookie 不出现在日志/API/审计；通过网络白名单和敏感字段扫描验证
- [ ] 6.3 执行现有 Paper 回归测试：Paper 下单/撤单、账务、持仓、风控熔断、跟盘和审计继续通过，且新增数据模块不存在 Broker/QMT/CTP/LIVE Bean 或调用路径；通过现有测试套件和装配 Bean 检查验证
- [ ] 6.4 执行架构和构建门禁：全模块 Maven 编译、ArchUnit、OpenAPI/事件/错误码校验、Flyway/ID/五末尾日志字段检查、DuckDB schema 检查全部通过；输出模块一落地报告并同步技术分册状态

## Workflow follow-up

- 实现完成后运行 `/openspec-verify-change`，对照本 change 的 specs、design 和 tasks 验证代码、配置、契约和测试。
- 验证通过后运行 `/openspec-archive-change`，同步能力 spec 并归档本 change。
