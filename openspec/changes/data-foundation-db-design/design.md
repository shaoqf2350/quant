# Design

## Context

本设计承接 `proposal.md` 与四份 capability spec，目标是把详设 1 的模块一落到可施工的数据库边界。当前仓库的数据库基线是 MySQL + Flyway `V1_0_0`~`V1_9_0`：已存在 `quantpf_instrument`、旧版 `quantpf_trading_calendar`、`quantpf_market_bar`、`quantpf_research_news`、`quantpf_financial_statement`、交易/风控/审计表；若依底座提供 `sys_user`、`sys_role`、`sys_menu`、`sys_dict_*`、`sys_config`、`sys_oper_log`、`sys_message`、`sys_notice`、`sys_oss` 等表。`quantpf-marketdata`、`quantpf-research` 和 `quantpf-common` 尚无业务 Java 源代码，因此本 change 只冻结持久化设计、迁移边界、契约和验收任务，不宣称代码已落地。

设计约束：

- MySQL 是除 K 线/Tick 外所有模块一数据的权威库；DuckDB 是 K 线/Tick 的权威时序库，数据文件挂 Docker volume。
- DuckDB K 线周期只允许 `30m`、`1d`、`1w`、`1M`；一期业务运行以日线为主。盘口摘要不进入 DuckDB。
- 所有时间以 `DATETIME(6)` 保存 UTC 瞬时值，同时保存市场本地日期/时区字段；不能用服务器时区推导交易日。
- 金额/价格/数量使用 `DECIMAL`，不使用浮点作为持久化主值；结构化扩展字段使用 JSON，但不能用 JSON 替代可检索主字段、状态、版本、授权和点时列。
- **MySQL 业务表主键统一使用 `id BIGINT NOT NULL`，禁止 `AUTO_INCREMENT`。** MyBatis-Plus 管理的实体主键使用 `IdentifierGenerator` 生成雪花 ID；业务侧需要主动发号的业务 ID 使用 Hutool `Snowflake` 生成。两套生成器必须共用同一套 `workerId/datacenterId` 配置，避免跨生成器碰撞；主键生成在应用侧完成，数据库只负责 `PRIMARY KEY`/唯一约束。
- **所有 MySQL 业务表的列末尾必须统一追加五个日志字段，顺序固定且不得插入业务字段之前：** `create_by BIGINT NULL`（创建人 ID，逻辑关联 `sys_user.user_id`）、`create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`（创建时间，UTC）、`update_by BIGINT NULL`（修改人 ID，逻辑关联 `sys_user.user_id`）、`update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)`（修改时间，UTC）、`remark VARCHAR(500) NULL`（备注）。这五列是所有新建表和后续演进的 QuantPF MySQL 业务表的统一公共列；`create_by/update_by` 允许系统任务使用约定的系统用户 ID 或 NULL，但不得伪造普通用户。
- 既有 QuantPF 表中历史 `created_at/updated_at` 列暂时保留以兼容现有代码和历史数据；新迁移必须在列末尾追加上述规范五列，并将应用写入切换到规范列。后续可在独立兼容变更中弃用旧列；不得修改已发布的历史 Flyway 文件。既有表主键的 `AUTO_INCREMENT` 移除也必须通过新的兼容迁移完成，不能回写历史迁移。
- **统一主键迁移范围覆盖现有全部 QuantPF MySQL 业务表，不仅是模块一新增表或 `quantpf_instrument`：**包括现有标的、日历、行情、新闻、财报、策略、回测、账户、台账、持仓、Paper 订单/成交、风控、熔断、任务审计、操作审计和跟盘表；所有主键、外键/关联 ID 必须统一为有符号 `BIGINT`，父子列类型必须完全一致。存量正数 ID 保持数值不变，只转换列定义并移除自增属性。
- 由于现有表之间存在外键，主键类型迁移必须按“暂停写入/备份 → 删除或暂时禁用外键 → 先改父表与子表列类型 → 重建外键/唯一键 → 校验存量关联 → 恢复写入”的顺序执行；禁止只改父表主键而遗留 `BIGINT UNSIGNED` 子列。
- `created_at/updated_at` 不得作为新表的规范日志列名称；DuckDB 时序表不属于 MySQL 业务表，但仍须使用应用生成的 `BIGINT` 稳定业务 ID（不使用自增）并按 DuckDB schema 约定保存导入时间。
- 所有业务幂等键使用 `VARCHAR(128)` 并建立唯一约束；跨表外键只在同一 MySQL 库内建立。DuckDB 不建立跨库 FK，以 `instrument_id`/稳定业务 ID 进行应用层一致性校验。
- Dify 无数据库直连、无订单/账户/风险写权限。Dify 输入只由 Spring Boot 按快照引用组装；Dify 原始输出写入 AI 草稿表，不写事实、研究事件、行情、质量、日历或用途资格主表。
- 本设计与上游基线的对应关系为：总纲第 5 章（数据来源、授权、时间可见性、回溯）、第 16.2/16.8/16.9（采集、调度、事件）、第 23.6（研究/交易/验证三流）、第 25.2~25.4（Spring Boot/Dify 边界）；技术分册 26.3/26.4（DuckDB、ADR-9~12）、27~29（领域表/接口/事件）；详设 0 第 4/5/13/14 章（数据治理、Dify契约、职责边界、对象/事件）；详设 1 第 5~23 章（来源、快照、质量、身份、日历、事件、行情、容量、用途资格、Dify协作和迁移顺序）。如本设计与上述基线冲突，应先更新上级文档，不由实现人员自行兼容两套语义。

## Goals / Non-Goals

**Goals:**

- 形成可直接拆解为 Flyway 迁移、DuckDB schema 初始化、种子数据和契约任务的表级设计。
- 覆盖数据治理、标的/日历、行情/衍生数据、研究入口/Dify 四个能力域，并明确表的权威库、生命周期、约束、索引和 Dify 交互。
- 兼容现有 Paper MVP：不修改历史迁移；对旧日历/K 线表采用新增模型 + 回填 + 只读冻结。
- 支持点时回放、授权用途门禁、来源冲突、修订链、主备来源、字段级降级和二期量价预留。
- 让普通用户只看到数据就绪/可用性结论，专业数据字段由管理员和系统任务处理。

**Non-Goals:**

- 不实现 Java Entity/Mapper/Repository、采集适配器、Dify WebClient、REST API、XXL-JOB Handler 或前端页面。
- 不购买或接入 Tushare、财联社、新浪、雪球；施工阶段只建立 Mock 可用的目录和状态数据。
- 不创建真实交易账户、Broker/QMT/CTP 表或任何真实委托通道。
- 不在本设计冻结 Paper 手续费/滑点的最终参数、策略阈值、概率标签或二期自动择时算法。
- 不复制若依用户、角色、菜单、字典、通知、对象存储表；只声明复用关系。

## Decisions

### 1. 存储分层与表命名

采用“业务主记录 MySQL + 时序数据 DuckDB + 缓存 Redis（不在本 change 建表）”三层。MySQL 表统一 `quantpf_` 前缀；DuckDB 表使用同名或 `qpf_` 前缀并在 `quantpf_schema_version` 中管理。原始大对象优先放 MySQL 元数据，正文/二进制超过配置阈值时写若依 `sys_oss`，业务表保存 `oss_id/object_key/content_hash`，禁止把密钥或 Dify 凭据放入正文。

本 change 的 MySQL 表分组如下：

| 分组 | 表 |
|---|---|
| 数据源与治理 | `quantpf_vendor`、`quantpf_data_product`、`quantpf_data_interface`、`quantpf_data_field`、`quantpf_data_source`、`quantpf_source_group`、`quantpf_source_group_member`、`quantpf_collection_task`、`quantpf_collection_attempt`、`quantpf_data_raw_snapshot`、`quantpf_data_observation`、`quantpf_data_quality_issue`、`quantpf_data_point_in_time`、`quantpf_data_use_eligibility` |
| 标的与日历 | `quantpf_instrument_version`、`quantpf_future_product`、`quantpf_instrument_relation`、`quantpf_trading_calendar_def`、`quantpf_trading_calendar_day`、`quantpf_trading_session_template`、`quantpf_instrument_calendar_binding`、`quantpf_market_data_readiness`、`quantpf_calendar_source_snapshot`、`quantpf_calendar_conflict`、`quantpf_corporate_action`、`quantpf_contract_event`、`quantpf_fx_session_rule` |
| 行情与衍生 | `quantpf_market_quote_snapshot`、`quantpf_market_state`、`quantpf_capacity_assessment`、`quantpf_quant_feature_snapshot`、`quantpf_supply_demand_observation`、`quantpf_basis_term_structure`、`quantpf_spot_quote` |
| 研究与 Dify | `quantpf_research_material`、`quantpf_research_material_relation`、`quantpf_research_task`、`quantpf_research_task_run`、`quantpf_dify_workflow_registry`、`quantpf_dify_invocation`、`quantpf_ai_research_draft`、`quantpf_research_event`、`quantpf_event_evidence` |
| DuckDB | `qpf_kline_bar`、`qpf_tick_data`、`qpf_schema_version` |

目标约 44 张新表；实际迁移时可将纯关联/尝试表合并到同一 Flyway 版本，但不得合并其业务语义。

- `quantpf_schema_marker`、DuckDB `qpf_schema_version`、Flyway 元数据和若依 `sys_*` 基础表不属于本规范所称的 QuantPF MySQL 业务表：它们不强制套用五个业务日志字段，但不得使用自增主键扩展新业务数据；若后续需要记录操作人，按其自身 schema/底座规范维护。

下列表是底座已有表，不在 QuantPF 迁移中重复建表或修改结构。QuantPF 仅保存其稳定 ID，并通过应用层按 owner/scope 查询；Dify 不得访问这些表。

| 表 | 复用字段（类型/约束） | 既有索引/关系 | 模块一用途与 Dify 交互 |
|---|---|---|---|
| `sys_user` | `user_id BIGINT PK`、`user_name VARCHAR(30) NOT NULL UNIQUE`、`status CHAR(1)`、审计时间字段 | PK、用户名唯一 | 操作者/审核人/owner；Dify 输入只传脱敏角色或用户请求上下文，不传密码/Token |
| `sys_role` | `role_id BIGINT PK`、`role_key VARCHAR(100) UNIQUE`、`status CHAR(1)` | PK、role_key | 数据管理员、审核员、风险管理员权限来源；Dify不读取 |
| `sys_menu` | `menu_id BIGINT PK`、`menu_name`、`perms VARCHAR(100)`、`status` | PK、权限字段索引 | 数据源/质量/日历/研究菜单；Dify无权限继承 |
| `sys_user_role` | `user_id BIGINT`、`role_id BIGINT`，组合关系 | 组合索引/唯一关系 | 授权判断输入；不复制到 QuantPF |
| `sys_dict_type` / `sys_dict_data` | 字典类型/数据主键、字典类型、字典值、状态 | 字典类型索引 | 展示标签与枚举翻译；状态主值仍由 QuantPF CHECK/应用校验维护 |
| `sys_config` | 配置键唯一、配置值、类型/备注 | config_key 唯一 | 仅保存非敏感开关；Token/Dify Key 不得放此表明文 |
| `sys_oper_log` | 操作人、模块、方法、请求参数、结果、时间 | 操作时间/模块索引 | 通用 UI 操作日志；关键数据状态迁移另写 `quantpf_audit_record` |
| `sys_oss` / `sys_oss_config` | 对象 ID、文件名、URL/对象键、配置 ID | PK、对象键索引 | 原始快照/附件超阈值时保存对象引用；Dify只能消费经过授权的片段引用 |
| `sys_notice` / `sys_message` | 通知 ID、接收人、内容、状态、时间 | 接收人/状态索引 | 数据质量、研究任务和 Dify 降级的站内通知；不放凭据和完整原文 |

### 3. 已有 QuantPF 表的演进与冻结

历史 Flyway `V1_0_0`~`V1_9_0` 不修改。新迁移只增加列、索引、兼容视图/触发防护或新表。

| 已有表 | 当前问题 | 新迁移处理 | Dify交互 |
|---|---|---|---|
| `quantpf_instrument` | 缺少 market/timezone、具体/连续/现货类型、来源/版本、product_id，以及规范日志字段；历史主键为自增无符号类型 | 新增兼容迁移：存量 ID 校验后转换为 `BIGINT NOT NULL`、移除 `AUTO_INCREMENT`；增加 `instrument_kind`、`market_code`、`timezone`、`currency`、`future_product_id`、`source_id`、`identity_status`、`current_version_id`；列末尾追加 `create_by/create_time/update_by/update_time/remark`；新属性写 `quantpf_instrument_version` | Dify只读稳定 `instrument_id` 和展示字段；不得修改身份 |
| `quantpf_trading_calendar` | 旧表将日期、开闭市、时段混在一起，且历史表使用自增主键/旧式日志列 | 回填至 `quantpf_trading_calendar_def/day`；新写入禁用；表保留只读查询；如进入兼容迁移范围，按统一 BIGINT/五日志字段规则转换，不修改历史 Flyway | Dify只能解释已计算的开闭市，不读写旧表 |
| `quantpf_market_bar` | MySQL K 线与 ADR-9 冲突，且历史表使用自增主键/旧式日志列 | 回填历史到 DuckDB；迁移后标记兼容只读，不再写入；如进入兼容迁移范围，先完成 ID/五日志字段转换再冻结 | Dify不直接读取；Spring Boot传授权的结构化 K 线摘要 |
| `quantpf_research_news` | 仅新闻原始字段，缺 available/authorization/version，且历史主键/日志列需统一 | 增加 `available_at`、`effective_at`、`observed_at`、`revised_at`、`source_id`、`snapshot_id`、`quality_status`、`authorization_status`、`material_id`；追加统一五个日志字段并移除自增主键属性；最终材料主记录在 `quantpf_research_material` | Dify输入来自材料快照，不把该表直接暴露给 Dify |
| `quantpf_financial_statement` | JSON 财报缺点时/修订字段，且历史主键/日志列需统一 | 增加 `published_at`、`available_at`、`revision_no`、`snapshot_id`、`quality_status`、`authorization_status`、`current_flag`；追加统一五个日志字段并移除自增主键属性；数值分析引用点时版本 | Dify只消费 Spring Boot 组装的财报摘要/证据引用 |
| `quantpf_audit_record` | 已有 detail JSON 粗粒度，属于跨模块审计事实表 | 不破坏既有字段；后续可加 `trace_id/source/version/snapshot_id` 和统一五个日志字段，但不把 DDL 设计误写成当前已具备；敏感字段仍由应用清洗 | Dify调用只产生受控调用审计，不写通用审计表 |

### 4. MySQL 表级详细设计：数据源与治理

#### MySQL 业务表统一主键与末尾日志字段（适用于本设计列出的每一张 `quantpf_*` 表）

除若依底座 `sys_*` 表外，本设计列出的所有 MySQL 业务表均必须遵守以下统一结构：

1. 主键列固定为 `id BIGINT NOT NULL PRIMARY KEY`，不使用 `AUTO_INCREMENT`。主键值由应用生成：MyBatis-Plus 实体使用 `IdentifierGenerator`；业务侧需要手动发号的业务 ID 使用 Hutool `Snowflake`。两者共用同一套 `workerId/datacenterId` 配置，并通过统一配置校验确保不同实例不重复。
2. 以下五个日志字段必须位于**表列定义末尾**，顺序固定：

   | 顺序 | 字段 | 类型/约束 | 含义 |
   |---:|---|---|---|
   | 1 | `create_by` | `BIGINT NULL` | 创建人 ID，逻辑关联 `sys_user.user_id`；系统任务可使用约定系统用户 ID 或 NULL |
   | 2 | `create_time` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)` | 创建时间，按 UTC 保存 |
   | 3 | `update_by` | `BIGINT NULL` | 最后修改人 ID，逻辑关联 `sys_user.user_id`；系统任务可使用约定系统用户 ID 或 NULL |
   | 4 | `update_time` | `DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)` | 最后修改时间，按 UTC 保存 |
   | 5 | `remark` | `VARCHAR(500) NULL` | 备注 |

3. 下文各表的字段栏只列业务字段和表专属字段；每张 MySQL 表在实际 DDL 中都必须在这些字段之后追加上述五列。除非另有明确说明，表格中的 `created_at/updated_at` 旧式写法均视为需要迁移为 `create_time/update_time`，不再新增同名日志列。
4. 外键/关联 ID（如 `vendor_id`、`source_id`、`instrument_id`）统一使用 `BIGINT NOT NULL/NULL`，与主键雪花 ID 类型一致；不使用 `UNSIGNED AUTO_INCREMENT`。

以下字段表使用 `BIGINT NOT NULL` 主键；公共五个日志字段按上述规则追加在每张 MySQL 表列末尾。所有 FK 均为逻辑 FK 或同库物理 FK，迁移时按依赖顺序创建。`JSON` 字段必须同时有可检索的摘要列/状态列。

MySQL 新表的每个业务字段表都遵守“业务字段 → 表专属系统字段（如 started_at、published_at、valid_from 等）→ 统一五个末尾日志字段”的列顺序。索引中的 `create_time` 代表规范创建时间；不再为新表定义 `created_at`。

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_vendor` | `id BIGINT PK`；`vendor_code VARCHAR(64) NN`；`name VARCHAR(128) NN`；`account_scope VARCHAR(32) NN`；`contract_ref VARCHAR(128)`；`status VARCHAR(24) NN`；`service_start/end DATETIME(6)`；`terms_url VARCHAR(512)`；`notes JSON`；**末尾追加统一五个日志字段** | UK(code)；IX(status, end) | 禁止直读；Spring Boot仅将授权状态摘要传入 |
| `quantpf_data_product` | `id`；`vendor_id FK NN`；`product_code/name VARCHAR(128) NN`；`package_ref`；`quota_limit DECIMAL(24,8)`；`quota_unit`；`history_from/to DATE`；`latency_ms INT`；`save_scope VARCHAR(32)`；`share_scope VARCHAR(32)`；`status`；`expires_at` | UK(vendor, code)；IX(status, expires_at) | 禁止直读；Dify不接触采购合同 |
| `quantpf_data_interface` | `id BIGINT`；`product_id BIGINT NOT NULL`；`interface_code/name VARCHAR(128) NOT NULL`；`endpoint_ref VARCHAR(256)`；`transport VARCHAR(16)`；`request_schema_version`；`response_schema_version`；`frequency_limit INT`；`concurrency_limit INT`；`permission_status`；`verification_status`；`status`；`raw_doc_ref`；**末尾追加统一五个日志字段** | UK(product, code, version)；IX(status, verification_status) | 不传 endpoint/凭据；可将已授权的接口能力摘要作为研究上下文元数据 |
| `quantpf_data_field` | `id`；`interface_id FK NN`；`field_code/name VARCHAR(128) NN`；`data_type VARCHAR(32)`；`unit/currency/timezone VARCHAR(32)`；`nullable_flag TINYINT`；`source_type`；`required_flag`；`availability_status`；`save_allowed/share_allowed`；`description`；`raw_field_name` | UK(interface, field, schema_version)；IX(availability_status) | Dify不得自行推断字段；仅返回允许使用字段的白名单 |
| `quantpf_data_source` | `id`；`source_code/name VARCHAR(128) NN`；`vendor/product/interface_id`；`source_level VARCHAR(8)`；`source_kind`；`authorization_status`；`usage_scope`；`coverage JSON`；`health_status`；`last_success_at/last_failure_at`；`failure_count INT`；`fallback_source_id`；末尾追加统一五个日志字段 | UK(source_code)；IX(kind,status,priority)；IX(expires/health) | 禁止直连；研究任务只接收 source_id、授权用途和健康摘要 |

状态 CHECK：vendor/product `DRAFT/PENDING_CONFIRMATION/ACTIVE/DEGRADED/EXPIRING/EXPIRED/SUSPENDED/RETIRED`；interface/field 的 `verification_status` 包括 `PENDING/VERIFIED/UNAVAILABLE/CHANGED`；`authorization_status` 包括 `UNKNOWN/PENDING/ALLOWED/BLOCKED/EXPIRED`。`usage_scope` 至少支持 `PERSONAL_RESEARCH/TEAM_INTERNAL/COMMERCIAL_SERVICE/NO_REDISTRIBUTION`。

#### 4.2 来源组、采集与快照

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_source_group` | `id`；`group_code/name VARCHAR(128) NN`；`material_type`；`required_primary_count TINYINT`；`required_backup_count TINYINT`；`status`；`cross_check_rule JSON`；`fallback_rule JSON`；末尾追加统一五个日志字段 | UK(group_code)；IX(material_type,status) | 禁止直读；Dify只看到来源数量/冲突摘要 |
| `quantpf_source_group_member` | `id`；`group_id/source_id FK NN`；`role VARCHAR(16)`；`priority INT`；`coverage JSON`；`authorization_status`；`active_from/to DATETIME(6)`；`last_health_at` | UK(group, source, role, priority)；IX(source,status) | 不传主备凭据；输出引用仅使用 source_id/material ref |
| `quantpf_collection_task` | `id`；`task_key VARCHAR(128) NN`；`task_type`；`trigger_type`；`source_id/group_id`；`scope JSON`；`status`；`idempotency_key NN`；`attempt_count`；`success_scope/failure_scope JSON`；`scheduler_execution_id`；`started_at/finished_at`；`error_code/message` | UK(idempotency_key)；IX(status, create_time)；IX(source, task_type, started_at) | Dify不创建采集任务；研究任务只引用采集完成状态 |
| `quantpf_collection_attempt` | `id`；`task_id FK`；`attempt_no INT NN`；`status`；`request_hash`；`response_status INT`；`started/finished_at`；`error_code/message`；`raw_snapshot_id` | UK(task, attempt_no)；IX(status, started_at) | 禁止交给 Dify；只用于解释数据是否就绪 |
| `quantpf_data_raw_snapshot` | `id`；`source_id/interface_id`；`external_id`；`request_hash CHAR(64)`；`content_hash CHAR(64) NN`；`content_type`；`raw_size BIGINT`；`raw_inline MEDIUMTEXT`（阈值内可选）；`oss_id/object_key`；`request_params JSON`；`response_headers JSON`（清洗后）；`published_at/available_at/effective_at/observed_at/revised_at`；`parser_version`；`authorization_status`；`quality_status`；`created_at` | UK(source_id, content_hash)；IX(external_id, source_id)；IX(available_at)；IX(content_hash) | Dify禁止访问原始库；Spring Boot只向已授权 Workflow 提供片段/引用，完整响应不出库边界 |
| `quantpf_data_observation` | `id`；`snapshot_id FK`；`observed_at`；`response_hash`；`duplicate_kind`；`collector_run_id`；`is_primary TINYINT`；`parse_status` | UK(snapshot, observed_at, collector_run_id)；IX(response_hash) | 不传；用于解释重复、转载和来源独立性 |

`raw_inline` 不存 Token、Authorization、Cookie、密码；所有自由文本在入库前递归清洗。`raw_snapshot` 的业务引用通过 snapshot_id + content_hash 形成可回放链。

#### 4.3 质量、点时和用途资格

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_data_quality_issue` | `id`；`issue_code/type`；`severity`；`status`；`object_type/object_id`；`source_id/snapshot_id`；`expected/actual JSON`；`impact_scope JSON`；`opened_at/resolved_at`；`assignee_user_id`；`resolution/reason`；`resolution_version` | IX(status,severity,create_time)；IX(object_type,object_id,status)；IX(source_id,status) | 只传用户可读原因和影响，不传敏感原始响应；Dify不能关闭问题 |
| `quantpf_data_point_in_time` | `id`；`object_type/object_id`；`snapshot_id`；`version_no INT`；`published_at/available_at/effective_at/observed_at/revised_at`；`decision_cutoff_at`；`quality_status`；`authorization_status`；`current_flag`；`supersedes_id`；`revision_reason` | UK(object_type, object_id, version_no)；IX(object_type,object_id,available_at)；IX(current_flag) | Dify输入必须带 snapshot/version/as_of；不得提交未来版本 |
| `quantpf_data_use_eligibility` | `id`；`object_type/object_id`；`point_in_time_id`；七个资格 `display_allowed/research_allowed/screening_allowed/strategy_allowed/backtest_allowed/paper_allowed/risk_monitoring_allowed TINYINT`；`reason_codes JSON`；`rule_version`；`calculated_at`；`expires_at` | UK(object_type,object_id,point_in_time_id,rule_version)；IX(paper_allowed,calculated_at)；IX(research_allowed) | Dify只读取资格摘要，不可改变任何资格；输出校验必须引用其 ID |

资格计算不得只依赖 `current_flag`；回测查询以 `available_at <= decision_time` 和授权用途共同过滤。质量问题未关闭时，资格表必须有明确阻断理由。

### 5. MySQL 表级详细设计：标的与日历

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_instrument_version` | `id`；`instrument_id FK`；`version_no`；`instrument_kind`；`exchange_code/market_code`；`currency/timezone`；`tick_size/lot_size/contract_multiplier/margin_rate DECIMAL(24,8)`；`limit_rule_json`；`trading_rule_json`；`listed_date/delisted_date`；`status`；`valid_from/to`；`current_flag`；`source_id/snapshot_id`；`change_reason`；**末尾追加统一五个日志字段** | UK(instrument,version_no)；以 `current_flag=1` 的过滤唯一索引/生成列保证单一当前版本；IX(exchange,kind,status); IX(valid_from,valid_to) | 只读身份/规则摘要；Dify不能修改交易资格或规则 |
| `quantpf_future_product` | `id`；`product_code/name`；`exchange_code`；`calendar_code`；`contract_multiplier/tick_size/default_margin_rate`；`delivery_rule_json`；`limit_rule_json`；`source_strategy`；`gfex_manual_flag`；`status`；末尾追加统一五个日志字段 | UK(exchange_code,product_code)；IX(calendar_code,status) | 可传品种解释上下文；GFEX 人工标志为只读 |
| `quantpf_instrument_relation` | `id`；`from_instrument_id/to_instrument_id`；`relation_type`（CONTINUOUS_TO_CONTRACT/UNDERLYING/ALIAS/REPLACED_BY）；`ratio/price_adjustment`；`valid_from/to`；`source_id` | UK(from,to,type,valid_from)；IX(to,type) | Dify可读映射候选，但不能把连续合约映射成可执行身份 |
| `quantpf_trading_calendar_def` | `id`；`calendar_code/name`；`market_type/region_code/venue_code`；`timezone/currency`；`calendar_level`；`source_policy`；`effective_from/to`；`version_no`；`status` | UK(calendar_code,version_no)；IX(venue_code,market_type,status) | Dify只能解释计算结果；不接受自然语言覆盖 |
| `quantpf_trading_calendar_day` | `id`；`calendar_id`；`market_local_date DATE`；`is_open TINYINT`；`day_status`；`previous_trade_date/next_trade_date`；`holiday_code/special_reason`；`source_id/source_record_hash`；`quality_status`；`version_no`；`valid_from/to`；`current_flag` | UK(calendar_id,market_local_date,version_no)；IX(calendar_id,market_local_date,current_flag)；IX(day_status) | 输入上下文可含日期、时区、is_open 和质量；Dify不可写 |
| `quantpf_trading_session_template` | `id`；`calendar_id`；`session_type`；`sequence_no`；`start/end_local_time`；`crosses_midnight`；`trading_day_assignment`；`is_tradable`；`valid_from/to`；`version_no` | UK(calendar,version,sequence)；IX(calendar,session_type) | 只读时段解释 |
| `quantpf_instrument_calendar_binding` | `id`；`instrument_id/calendar_id`；`binding_type`；`tradability_status`；`valid_from/to`；`source/reason`；`version_no`；`current_flag` | UK(instrument,calendar,valid_from)；IX(calendar,tradability_status) | Dify只读取最终资格与阻断原因 |
| `quantpf_market_data_readiness` | `id`；`calendar_code`；`market_local_date`；`expected_flag/received_flag/validated_flag/strategy_ready_flag/paper_ready_flag`；`last_valid_snapshot_id`；`block_reason`；`version_no`；末尾追加统一五个日志字段 | UK(calendar_code,market_local_date,version_no)；IX(paper_ready_flag,market_local_date) | 只传“可研究/等待数据/暂不生成模拟建议”摘要 |
| `quantpf_calendar_source_snapshot` | `id`；`calendar_id/day_id`；`source_id`；`endpoint_name`；`request_params JSON`；`raw_payload_hash/raw_payload_ref`；`source_schema_version`；`retrieved_at`；`permission_profile`（脱敏） | UK(source_id,endpoint_name,raw_payload_hash)；IX(calendar_id,retrieved_at) | Dify不可读原始权限 profile；仅传来源/时间摘要 |
| `quantpf_calendar_conflict` | `id`；`calendar_id/day_id`；`conflict_type`；`source_a/source_b`；`value_a/value_b JSON`；`status`；`impact_scope JSON`；`override_value`；`override_reason`；`operator_user_id`；`valid_until` | IX(status,create_time)；IX(day_id,status) | Dify只能解释冲突，不可自行选择值 |
| `quantpf_corporate_action` | `id`；`instrument_id`；`action_type`；`announcement_at/effective_at`；`payload JSON`；`source_id/snapshot_id`；`status`；`revision_no`；`impact_scope JSON` | IX(instrument_id,effective_at)；IX(action_type,status) | 只读事件摘要/证据引用 |
| `quantpf_contract_event` | `id`；`instrument_id/future_product_id`；`event_type`；`event_at/effective_at`；`payload JSON`；`source_id/snapshot_id`；`status`；`impact_scope JSON`；`revision_no` | IX(instrument_id,effective_at)；IX(event_type,status) | 只读交割/移仓/保证金事件摘要 |
| `quantpf_fx_session_rule` | `id`；`instrument_id`；`broker_code`；`classify`；`market_timezone`；`trading_hours/break_time`；`min_stop_distance/pip/pip_cost`；`valid_from/to`；`source_id`；`status` | UK(instrument,broker,valid_from)；IX(classify,status) | 仅 schema/时段解释预留；一期不开放 FX 采集/交易 |

一期种子日历：`CN_STOCK_SSE`、`CN_STOCK_SZSE`、`CN_STOCK_BSE`、`CN_FUTURE_SHFE`、`CN_FUTURE_DCE`、`CN_FUTURE_CZCE`、`CN_FUTURE_INE`、`CN_FUTURE_CFFEX`、`CN_FUTURE_GFEX`。GFEX 日历只能人工录入并版本化，不能由缺失接口推断。

### 6. MySQL 表级详细设计：行情与衍生数据

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_market_quote_snapshot` | `id`；`instrument_id`；`quote_time/market_local_date`；OHLC/volume/amount；`turnover_rate/relative_volume/volume_zscore/pe_ttm/float_share/market_value`；`bid/ask/spread/depth`；`upper/lower_limit`；`field_availability JSON`；`source_id/snapshot_id`；`quality_status`；`point_in_time_id` | UK(instrument,quote_time,source_id)；IX(instrument,quote_time)；IX(market_local_date,quality_status) | Dify只消费经权限过滤的指标摘要；不得访问盘口原始库或生成信号 |
| `quantpf_market_state` | `id`；`scope_type/scope_id`；`market_local_date`；`state`；`feature_snapshot_id`；`rule_version`；`input_snapshot_id`；`reason JSON`；`valid_until` | UK(scope_type,scope_id,date,rule_version)；IX(state,date) | Dify可解释理由，不能改 state |
| `quantpf_capacity_assessment` | `id`；`instrument_id`；`market_local_date`；`status`；`avg_turnover/avg_volume`；`spread/depth`；`planned_amount_ratio`；`slippage_estimate`；`stop_executable_flag`；`rule_version`；`input_snapshot_id`；`reason JSON` | UK(instrument,date,rule_version)；IX(status,date) | 只读风险摘要；Dify不能放宽容量门禁 |
| `quantpf_quant_feature_snapshot` | `id`；`instrument_id`；`market_local_date`；`timeframe`；`feature_definition_version`；`feature_values JSON`；`state`；`market_state_id`；`point_in_time_id`；`source_snapshot_id`；`label_status`；末尾追加统一五个日志字段 | UK(instrument,date,timeframe,feature_version)；IX(feature_version,market_local_date)；IX(state) | Dify只读已授权结构化特征并解释；不能写特征/标签 |
| `quantpf_supply_demand_observation` | `id`；`commodity_id/instrument_id`；`region/grade`；`data_type`；`value DECIMAL(30,12)`；`unit`；`period_end`；`published_at/available_at`；`source_id/snapshot_id`；`quality_status`；`seasonal_percentile`；`revision_no`；`current_flag` | UK(commodity,region,grade,type,period_end,revision_no)；IX(available_at)；IX(type,period_end) | Dify可消费已授权供需摘要和证据引用，不能补造缺失值 |
| `quantpf_basis_term_structure` | `id`；`commodity_id/spot_instrument_id/future_instrument_id`；`observation_date`；`spot_price/future_price/basis`；`term_structure_json`；`unit/currency`；`published_at/available_at`；`source_id/snapshot_id`；`quality_status`；`revision_no`；`current_flag` | IX(commodity,observation_date)；IX(future_instrument_id,observation_date)；UK(legs,date,revision_no) | 只读研究上下文；Dify不生成价差/订单 |
| `quantpf_spot_quote` | `id`；`commodity/grade/region/location`；`price_type`；`quoted_at`；`price/unit/currency`；`trade_condition`；`inventory_exposure_flag`；`source_id/snapshot_id`；`quality_status`；`available_at` | IX(commodity,region,quoted_at)；IX(source_id,available_at) | 只读研究输入；无敞口时明确传 `inventory_exposure=false`，不能派生现货空单 |

盘口摘要的字段可得性以 JSON 存详细字段，同时 `quality_status` 和 `use_eligibility` 是硬门禁列。高频原始数据不进入 MySQL 盘口表；一期没有实时 Tick UI。

### 7. MySQL 表级详细设计：研究入口与 Dify

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `quantpf_research_material` | `id`；`material_type`；`title`；`summary`；`source_id/group_id`；`raw_snapshot_id`；`external_id`；`content_hash`；`published_at/available_at/effective_at/observed_at/revised_at`；`status`；`quality_status`；`authorization_status`；`dedup_kind`；`instrument_refs JSON`；`current_version`；末尾追加统一五个日志字段 | UK(source,external_id)；UK(source,content_hash)；IX(type,published_at)；IX(status,available_at) | Dify输入为 Spring Boot 选定的 material_id/version/授权片段；Dify不得改材料 |
| `quantpf_research_material_relation` | `id`；`from_material_id/to_material_id`；`relation_type`（转载/重复/改写/引用/修订）；`confidence`；`source_id`；**末尾追加统一五个日志字段** | UK(from,to,type)；IX(to,type) | Dify可见关系摘要，不可宣称转载为独立证据 |
| `quantpf_research_task` | `id`；`task_key`；`task_type`；`trigger_type`；`material_refs/event_refs/subject_refs/instrument_refs JSON`；`snapshot_id`；`published_at/available_at/as_of_time`；`authorization_status/quality_status/use_eligibility`；`research_question/period`；`workflow_version/prompt_version/schema_version`；`status`；`idempotency_key`；`owner_user_id`；末尾追加统一五个日志字段 | UK(idempotency_key)；IX(status,create_time)；IX(task_type,as_of_time) | Spring Boot创建；Dify只消费最小上下文，不接账户/交易凭据 |
| `quantpf_research_task_run` | `id`；`task_id`；`run_no`；`status`；`invocation_id`；`retry_of`；`started/finished_at`；`error_code/message`；`output_draft_id`；`audit_ref` | UK(task_id,run_no)；IX(status,started_at) | 记录 Dify/确定性运行，不由 Dify改状态 |
| `quantpf_dify_workflow_registry` | `id`；`workflow_key`；`dify_app_ref`（非 Key）；`workflow_version`；`model_ref/version`；`prompt_version`；`input/output_schema_version`；`status`；`timeout_seconds`；`max_input/output`；`published_at`；`approved_by`；`environment` | UK(workflow_key,version,environment)；IX(status,environment) | Dify配置元数据；密钥只在环境变量/密钥系统，表不存明文 |
| `quantpf_dify_invocation` | `id`；`invocation_id`；`task_id/task_run_id`；`workflow_registry_id`；`dify_run_id`；`request_hash/response_hash`；`request_ref/response_ref`；`response_mode`；`status`；`http_status`；`started/finished_at`；`token_usage JSON`；`error_code/message`；`retry_of`；`idempotency_key` | UK(invocation_id)；UK(idempotency_key)；IX(task_id,status)；IX(dify_run_id) | 仅 Spring Boot 适配器写入；记录流式消费/超时/限流；原始载荷须脱敏 |
| `quantpf_ai_research_draft` | `id`；`task_run_id`；`invocation_id`；`raw_response_ref`；`structured_payload JSON`；`schema_status`；`citation_status`；`time_visibility_status`；`authorization_status`；`subject_status`；`conflict_status`；`overall_status`；`failure_reasons JSON`；`model/workflow/prompt/schema versions`；**末尾追加统一五个日志字段** | UK(invocation_id)；IX(overall_status,create_time)；IX(task_run_id) | 这是 Dify 输出承载表；Dify不可直接写库，Spring Boot保存原响应并校验，草稿不作为事实 |
| `quantpf_research_event` | `id`；`event_key`；`event_type`；`impact_scope/level`；`direction_candidate`；`verified_direction`；`status`；`material_id/draft_id`；`version_no`；`previous_event_id`；`published_event_id`；`valid_from/to`；`verified_by`；`verification_reason`；末尾追加统一五个日志字段 | UK(event_key,version_no)；IX(status,published_at)；IX(event_type,available_at) | 只由 Spring Boot 写入；Dify只能提供草稿候选，不可修改事件 |
| `quantpf_event_evidence` | `id`；`event_id`；`evidence_type`；`material_id/snapshot_id/source_id`；`quote_text/quote_location`；`structured_field_ref`；`content_hash`；`published_at/available_at`；`verification_status`；`independence_group`；**末尾追加统一五个日志字段** | IX(event_id)；IX(source_id,independence_group)；UK(event_id,content_hash,quote_location) | Dify输出的引用先作为 AI_EXTRACTED/待核验；用户备注是 USER_ANNOTATION，不得伪装外部证据 |

Dify 交互固定为：

```text
Spring Boot 查询/冻结材料、来源、身份、点时与用途资格
→ 生成最小输入 JSON（requestId/idempotencyKey/taskId/snapshot/version/引用）
→ 调用已发布 Dify Workflow API（streaming，Bearer Key 服务端注入）
→ 消费流式响应并保存 invocation/raw response
→ Schema/引用/时间/授权/身份/冲突校验
→ 保存 AI 草稿；通过后由 Spring Boot/人工流程升级研究事件
→ 发布 ResearchEventPublishedEvent
```

禁止 Dify 连接 MySQL、DuckDB、Redis、Paper、订单或审计数据库；禁止将 Dify 向量库作为事实、行情、点时或量价主库。

### 8. DuckDB 表级详细设计

DuckDB 迁移由应用启动时的 schema initializer 执行，使用同一事务/锁模型保证版本幂等；`qpf_schema_version` 是唯一 schema 事实源。

| 表 | 字段（类型；约束） | 索引/唯一键 | Dify 交互 |
|---|---|---|---|
| `qpf_schema_version` | `version VARCHAR(32) PK`；`description VARCHAR(256) NOT NULL`；`installed_at TIMESTAMP`；`checksum VARCHAR(64)` | PK(version)；UK(checksum,version) | 禁止交互 |
| `qpf_kline_bar` | `instrument_id BIGINT NOT NULL`；`instrument_code VARCHAR(64)`；`exchange_code`；`timeframe VARCHAR(8) CHECK IN ('30m','1d','1w','1M')`；`bar_time TIMESTAMP`；`market_local_date DATE`；`timezone VARCHAR(64)`；OHLC `DECIMAL(24,8)`；`volume/turnover DECIMAL(30,8)`；`open_interest DECIMAL(30,8)`；`adjust_flag VARCHAR(16)`；`source_code`；`source_snapshot_id BIGINT`；`point_in_time_id BIGINT`；`import_batch_id VARCHAR(128)`；`quality_status`；**导入时间字段按 DuckDB schema 约定保存（非 MySQL 五日志字段）** | UNIQUE(instrument_id,timeframe,bar_time,adjust_flag,source_code)；索引(instrument_id,timeframe,bar_time)、(market_local_date) | Dify不可直连；Spring Boot只提供授权的聚合/特征结果 |
| `qpf_tick_data` | `instrument_id BIGINT`；`instrument_code`；`exchange_code`；`tick_time TIMESTAMP`；`market_local_date DATE`；`timezone`；`last_price/bid_price/ask_price DECIMAL(24,8)`；`bid_volume/ask_volume DECIMAL(30,8)`；`volume/turnover/open_interest`；`source_code`；`source_snapshot_id`；`quality_status`；**导入时间字段按 DuckDB schema 约定保存（非 MySQL 五日志字段）** | UNIQUE(instrument_id,tick_time,source_code)；索引(instrument_id,tick_time) | 一期只作结构预留；Dify不能读取逐 Tick 原始数据或生成订单 |

DuckDB 不存盘口摘要、研究正文、Dify 草稿、因子、风险或账户数据。跨库一致性通过 `source_snapshot_id`/`point_in_time_id` 与 MySQL 校验；不存在物理 FK 是有意取舍，避免嵌入式库反向成为业务主库。

### 9. 迁移分批与依赖顺序

建议新增迁移（实际编号以施工时 Flyway 版本扫描结果为准，不能覆盖旧文件）：

1. `V1_10_0__data_catalog_and_governance`：vendor/product/interface/field/source/source_group/member。
2. `V1_11_0__collection_snapshot_quality`：collection_task/attempt/raw_snapshot/observation/quality_issue/point_in_time/use_eligibility。
3. `V1_12_0__instrument_version_and_future_product`：既有 instrument 加列、instrument_version/future_product/relation。
4. `V1_13_0__calendar_and_market_events`：新日历四层、readiness、source_snapshot/conflict、corporate_action/contract_event/fx_rule。
5. `V1_14_0__market_derived_data`：quote_snapshot/market_state/capacity/quant_feature/supply_demand/basis_term/spot_quote。
6. `V1_15_0__research_dify_entry`：material/relation/task/task_run/workflow_registry/invocation/draft/event/evidence，并演进旧 news/financial 表。
7. `V1_16_0__compatibility_backfill_and_indexes`：旧日历/K 线回填标记、只读兼容配置、数据域复合索引和 CHECK 补强。
8. **独立兼容迁移（版本号以实际依赖确定）**：盘点全部现有 QuantPF 业务表及其 FK/UK，统一主键/关联 ID 为有符号 `BIGINT`、移除 `AUTO_INCREMENT`，为全部现有业务表追加五个末尾日志字段并回填；必须先停写/备份、处理外键、重建约束后再恢复。
9. DuckDB `qpf_schema_version` v1 + `qpf_kline_bar`/`qpf_tick_data` schema，不通过 MySQL Flyway 执行。

种子数据与迁移分离：

- `quantpf_future_product` 种入 25 个一期品种，GFEX 两项绑定人工日历；
- `quantpf_data_source` 种入 Tushare/财联社/新浪/雪球，状态 `PENDING_CONFIRMATION`，不写 Token；
- 日历编码、质量状态、用途资格、研究/Dify状态使用 `sys_dict_*` 展示字典，同时数据库 CHECK 与应用枚举作为约束；
- `sys_menu`/权限码仅由正式菜单契约与种子脚本维护，重复执行必须幂等。

### 10. 索引、约束与分区策略

- 高频/范围查询优先复合索引：`(instrument_id, market_local_date)`、`(source_id, available_at)`、`(status, create_time)`、`(object_type, object_id, status)`。
- 版本表以 `(object_id, version_no)` 唯一，以 `current_flag` 和有效期索引查询当前/点时版本；**对于需要“仅一个当前版本”的表，必须使用 MySQL 支持的过滤唯一策略（如 `current_key` 生成列：current_flag=1 时取业务对象 ID，否则为 NULL，再对 `(object_id,current_key)` 建唯一索引），不能直接对 `(object_id,current_flag)` 建唯一键**；禁止依赖 `ORDER BY id` 推断最新。
- 所有状态列建立 CHECK 或由应用枚举校验；MySQL 版本若对 CHECK 执行能力不足，CI 必须用约束测试补齐。
- JSON 不建无边界全文索引；需要查询的字段提升为实体列。原始正文按大小/留存策略进入 `sys_oss`。
- 本 change 不引入 MySQL 分区，先按复合索引与日线规模运行；若数据量压测超过阈值，另行 ADR 决定按日期归档/分区，不能在一期隐式改变点时查询语义。
- DuckDB 按 `instrument_id/timeframe/market_local_date` 查询组织，必要时由导入批次做物理追加；唯一自然键保证重放幂等。

### 11. Dify 安全与数据交互矩阵

| 数据类别 | Dify是否可读 | 传递方式 | Dify是否可写 |
|---|---:|---|---:|
| 供应商 Token、Dify Key、用户密码、Paper/真实账户 | 否 | 不传 | 否 |
| 原始新闻/公告正文 | 受控 | Spring Boot 按授权传最小片段 + snapshot/material 引用 | 否 |
| 财报/研报/产业材料 | 受控 | 已授权字段/片段 + 点时/来源/版本 | 否 |
| K 线/盘口/Tick 原始表 | 否 | 只传必要聚合、特征和引用 | 否 |
| 量价特征/市场状态/容量 | 是（最小化） | 结构化 JSON，带 feature/rule/snapshot 版本 | 否 |
| 质量/授权/日历最终状态 | 是（摘要） | 状态、原因、下一步 | 否 |
| AI 原始响应/结构化草稿 | 由 Spring Boot 保存 | invocation/draft 关联 | Dify不直写 |
| 研究事件/证据主表 | 只读查询结果 | 受控回调/查询上下文 | 否 |

所有 Dify 输入输出均记录 workflow/model/prompt/schema 版本、调用 ID、任务 ID、snapshot ID 和哈希。Dify Workflow API 默认采用 Spring Boot 服务端消费 `text/event-stream` 流式响应；若部署版本实测支持可靠的 `callback_url + X-Dify-Signature` HMAC-SHA256 验签，可启用专用研究回调，但回调只能进入草稿校验队列，不能直接改业务状态。Dify 返回 `directionCandidate`、`normalizedSubjectHint`、`confidence` 均只作为草稿字段，不能进入 `verified_direction`、市场状态、用途资格、策略信号或订单。

### 12. 迁移计划与回滚

1. 在 CI 中使用全新 MySQL schema 执行 `V1_0_0`~新迁移，验证迁移顺序、CHECK、UK、索引和重复执行。
2. 在副本环境对现有表做只读盘点、回填计数、哈希比对；回填失败不切换读路径。
3. 先上线新增表与兼容读取，再启用新写路径；旧日历/K 线写入通过应用配置禁用而非删除旧表。
4. DuckDB 先初始化 schema，再导入可验证的 K 线；导入批次记录成功/失败范围，不能将失败批次标记完整。
5. 通过 Mock 数据跑数据源→快照→质量→点时→日历就绪→研究任务→Dify草稿（可用 stub）链路后，才允许下游模块接入。
6. 回滚：若新表迁移失败，Flyway 不执行后续版本；若业务切换失败，关闭新写路径并继续旧只读/兼容查询；不得回滚已成功写入的历史版本或删除新表数据。需要删除/反向迁移时必须单独审批并保留备份。
7. 迁移成功标准：旧表行数/哈希与回填结果一致；重复采集/重复任务不产生重复；点时读取不暴露未来版本；未授权/冲突/过期数据不具备 Paper 资格；Dify不可用不影响基础采集与确定性链路。

### 14. 雪花主键与五个末尾日志字段迁移/验收专项要求

本节是对所有 MySQL 业务表的强制统一备注，优先级高于单表字段简写：

- **主键**：每张业务表的 `id` 必须是 `BIGINT NOT NULL PRIMARY KEY`，禁止 `AUTO_INCREMENT`。MyBatis-Plus 实体使用最新配置的 `IdentifierGenerator`；业务侧确实需要手动发号的业务 ID 使用 Hutool `Snowflake`。二者共用同一套 `workerId/datacenterId` 配置和实例注册/校验机制。
- **末尾日志字段**：每张表的列定义最后五列必须严格为：
  `create_by BIGINT NULL`、
  `create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`、
  `update_by BIGINT NULL`、
  `update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)`、
  `remark VARCHAR(500) NULL`。
- **语义**：`create_by/update_by` 是创建人/修改人 ID，逻辑引用若依 `sys_user.user_id`；系统任务不得伪造普通用户身份，可使用约定系统用户 ID 或 NULL。时间统一按 UTC 保存。
- **新增表**：DDL 必须直接按该规则创建，不能再出现 `created_at`、`updated_at` 作为规范日志字段。
- **既有表**：不得修改历史 Flyway；新增迁移必须在末尾追加并回填五列，应用切换后再移除 `AUTO_INCREMENT`。历史 `created_at/updated_at` 仅作为兼容遗留列保留，不能继续作为新代码的写入目标。
- **跨库表**：DuckDB K 线/Tick 表不是 MySQL 业务表，但 ID 仍由应用侧生成 BIGINT，不使用自增；其导入时间按 DuckDB schema 约定保存。`qpf_schema_version.version` 是版本键，不属于业务实体雪花 ID 例外。

验收必须检查：DDL 中不存在 MySQL 业务表 `AUTO_INCREMENT`；每张 MySQL 业务表列尾五列顺序完全一致；`SHOW CREATE TABLE` 中主键和五列类型/默认值符合约定；MyBatis-Plus 与 Hutool 两种生成器在相同 worker/datacenter 配置下生成 ID 不碰撞。

- 数据治理组：供应商未授权、字段变更、主备切换、重复快照、部分采集、质量阻断、点时过滤。
- 标的日历组：同代码跨交易所、连续合约拒绝执行、GFEX人工日历、夜盘归属、日历冲突、日线未就绪、事件生效重算标记。
- 行情组：非法 OHLC、自然键去重、DuckDB重启持久化、盘口字段缺失降级、状态/容量回放、特征版本并存。
- 研究/Dify组：转载去重、任务幂等、流式调用重试、Schema/引用/时间/授权/身份/冲突校验、Dify宕机降级、研究事件重复发布。
- 安全组：Token/Authorization/Cookie 清洗、Dify不能访问数据库、草稿不能升级事实、审计关联完整。

## Risks / Trade-offs

- **[Risk] 表数量较多，首批迁移与维护成本高** → 按领域分批 Flyway；所有表先由模块一权威定义，后续模块扩展字段必须通过新迁移，禁止复制主表。
- **[Risk] MySQL 与 DuckDB 双库可能产生身份或点时引用不一致** → 使用稳定 `instrument_id`、`snapshot_id`、`point_in_time_id`；应用层导入前校验，增加跨库一致性集成测试。
- **[Risk] 既有表已经存在 `AUTO_INCREMENT` 主键或旧式 `created_at/updated_at` 列，直接替换会破坏现有代码兼容性** → 历史 Flyway 文件不回写；在新增兼容迁移中先补齐并回填五个规范日志字段，再切换应用写入；主键去自增前先完成应用侧雪花 ID 双读/写兼容、全量父子 FK 类型校验和存量数据校验，最后通过新迁移移除自增属性。
- **[Risk] 既有表仍被旧代码写入，破坏新存储/日志边界** → 新迁移只冻结兼容表；在应用配置、Repository 检查和测试中禁止旧表/K 线写入，并监控旧表写入计数。
- **[Risk] Dify 原始响应或材料正文泄露密钥/个人信息** → 入库前递归脱敏；Dify输入最小化；密钥仅环境变量/密钥系统；日志、审计、对象存储均做内容清洗。
- **[Risk] JSON 灵活性导致字段不可查询或质量不一致** → 状态、版本、来源、时间、资格、自然键和核心数值全部实体化；JSON 只承载供应商扩展和解释性 payload。
- **[Risk] Tushare/新闻源真实权限与设计不一致** → 施工期全 Mock；联调前逐接口/字段登记；未确认状态永远不能进入 Paper/回测资格。
- **[Risk] 日历来源冲突造成错误开闭市判断** → 冲突显式阻断；GFEX 人工维护版本化；不对空值默认开市；人工覆盖需有效期和审计。
- **[Risk] 把研究材料表误当事实表** → `research_material`、`ai_research_draft`、`research_event` 分层；Dify只产草稿，研究事件只能由 Spring Boot 核验流程写入。
- **[Risk] 过早做分区/高频优化锁定错误方案** → 一期仅复合索引和规模测试；性能不足时通过独立 ADR 调整，不改变业务自然键和点时语义。

## Migration Plan

迁移顺序、回填、兼容冻结与回滚策略见“迁移分批与依赖顺序”和“迁移计划与回滚”章节。实施时必须先完成 Phase 0 工具链/契约门禁，再执行数据库迁移；数据库设计完成不等于迁移已执行或模块已落地。

## Open Questions

以下事项不改变本设计的表边界或 Dify 隔离原则，可在实施前作为配置登记完成：

- Tushare 2000 积分套餐实际开通后的接口、字段、额度、延迟和保存权；
- 新闻源最终授权条款与原文长期保存策略；
- DuckDB 文件目录、备份频率与生产容量阈值；
- Paper 成交/滑点参数及模块四对 `quantpf_market_quote_snapshot` 的具体读取窗口。
