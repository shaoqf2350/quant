# Tasks

源码库根目录：前端 `[前端] = D:\workspace\quant\plus-ui`；后端 `[后端] = D:\workspace\quant\RuoYi-Vue-Plus`。

按用户指定顺序执行七个阶段。路径按当前仓库真实结构书写：后端 Flyway 权威目录为 `db/migration/`（不是独立的 `[后端]/sql/`）；种子/辅助 SQL 放 `script/sql/`。各模块的完整表、类及页面清单见本 change `design.md` 对应章节。

- [ ] **1. 数据库**：[后端]/db/migration/ 下创建模块一 MySQL 建表/兼容迁移脚本（数据源与来源组、采集与快照/质量/点时/用途资格、标的/期货品种/日历/时段/事件、盘口摘要/市场状态/容量/特征、研究材料/任务/Dify 草稿/研究事件/证据）；DuckDB schema 资源中创建 `qpf_schema_version`、`qpf_kline_bar`、`qpf_tick_data`，`[后端]/script/sql/` 创建幂等种子脚本。所有 QuantPF MySQL 业务表满足雪花 `BIGINT` 主键、禁用 `AUTO_INCREMENT`、父子 FK 类型统一、列末尾五字段 `create_by/create_time/update_by/update_time/remark`；旧历史迁移不回写。验证 Flyway 新库/现有库迁移、`SHOW CREATE TABLE`、约束/索引、父子 ID 类型、DuckDB 重启/幂等初始化、旧日历/K线回填和审计/Paper 表兼容性。

- [ ] **2. 后端实体**：[后端]/quantpf-modules/quantpf-domain/src/main/java/com/quantpf/domain/ 下创建数据库设计中数据源、快照、质量、点时、资格、标的/日历、行情/特征、研究任务/草稿/事件/证据对应的实体、值对象和 Repository 接口；在 `.../domain/persistence/` 下创建 Record、Mapper、XML；在 `[后端]/quantpf-modules/quantpf-common/src/main/java/com/quantpf/common/` 下统一 ID/Actor/日志字段类型。验证实体与 DDL 映射、雪花 ID、五日志字段自动填充、状态机和 MyBatis 集成测试。

- [ ] **3. 后端接口**：[后端]/quantpf-modules/quantpf-marketdata/src/main/java/com/quantpf/marketdata/ 下创建数据源、采集、快照、质量、点时资格、标的、日历、行情、DuckDB Adapter 应用服务；`[后端]/quantpf-modules/quantpf-research/src/main/java/com/quantpf/research/` 下创建材料、研究任务、草稿校验、研究事件/证据服务；`[后端]/ruoyi-admin/src/main/java/com/quantpf/web/controller/` 下创建数据源、质量、快照、标的、日历、市场、研究任务/事件 Controller 和 DTO；更新 `[后端]/contract/{openapi,events,error-codes,menu}/`。验证 Sa-Token/scope、幂等/expectedVersion、点时、质量阻断、连续合约拒绝、Paper 前置状态和 Controller 不直写 Mapper。

- [ ] **4. Dify 集成**：[后端]/quantpf-modules/quantpf-research/src/main/java/com/quantpf/research/dify/ 下编写 Workflow 注册、最小请求上下文、Dify WebClient、SSE 流消费、可选签名回调服务；在 `validation/` 实现 Schema/引用/时间/授权/身份/冲突校验；在 `[后端]/ruoyi-admin/src/main/resources/application*.yml` 与环境变量模板配置 Dify v1.16.1 URL、Workflow/环境、超时、重试、并发、限额和 callback secret。验证 Stub Dify SSE 完成/中断/超时、非法响应不升级事实、回调签名/重放、Key 不进日志/前端、Dify 不直连业务库。

- [ ] **5. 前端页面**：[前端]/src/views/quant/ 下创建数据健康工作台、数据源/来源组、质量/快照/就绪、标的中心、日历/冲突、一期日线行情、研究任务向导、AI 草稿/证据/研究事件、全局研究助手页面；在 `components/` 创建状态卡、数据新鲜度、证据抽屉、版本时间线、GuidedTask、Dify 运行状态组件；通过动态菜单接入既有 `contract/menu/quantpf-menu-facts.yaml`。验证管理员配置、审核冲突、普通用户低输入、任务恢复、Dify 降级、Paper 前置阻断与无绕过入口；页面不得出现分钟/Tick/盘中、LIVE/QMT/CTP/真实下单能力。

- [ ] **6. 前端接口**：[前端]/src/api/quant/ 下创建 dataSource、dataQuality、snapshot、instrument、calendar、market、researchTask、dify、researchEvent API 模块和 TypeScript types；Dify 按钮只调用 Spring Boot，不保存 Dify Key/URL；写操作传 requestId、Idempotency-Key、expectedVersion（适用时）和审核理由。验证 `plus-ui/package.json` 声明的 typecheck/lint/build 脚本、Mock API 合同、超时重复提交复用幂等键、浏览器网络无 Dify 直连。

- [ ] **7. 联调验证**：后端 `[后端]` 使用 JDK21 + Maven 启动 `DromaraApplication`（启动前核对 Wrapper、profile、端口、数据库及 DuckDB 配置），前端 `[前端]` 使用 `pnpm dev`（启动前核对 package.json scripts；需要时先 `pnpm install`）；使用 Mock 数据完成数据源→采集→快照→质量/点时/资格→标的/日历就绪→DuckDB 日线→研究任务→Dify SSE 草稿/校验→审核/研究事件→前端回显。验证 Dify 不可用降级、授权/数据冲突/日线过期阻断、现有 Paper 风控/订单/账务/审计回归、前后端权限 scope、安全扫描和一期边界，记录真实启动命令/结果并同步总纲索引、详设 1、技术分册及三个来源 change。

## Workflow follow-up

- 实现完成后运行 `/openspec-verify-change`，核对数据库、后端实体/接口、Dify、前端页面/API 与 WHEN/THEN 场景。
- 验证通过后运行 `/openspec-archive-change`，同步 capability specs 并归档整合 change。
