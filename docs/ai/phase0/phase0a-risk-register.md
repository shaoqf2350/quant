| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | ---- |
| P0-001 | P0 | Maven/Java 工具链不满足 V28 JDK21 门禁 | 根 POM `java.version=21`；`java -version`=OpenJDK 8.0.482；`mvn -version` Java=17.0.1 | 后端构建/运行基线不可信，编译可能直接失败 | 先建立 JDK21、Maven 和 JAVA_HOME 的人工批准基线，再运行任何构建 | 是 |
| P0-002 | P0 | QuantPF 工程骨架不存在 | 无 `quantpf-modules`、无 `com.quantpf.*`、无 `quantpf-web` | 没有合规业务代码落点，直接写业务会污染底座 | 按 V28-0 建立最小骨架，先不写业务实现 | 是 |
| P0-003 | P0 | SnailJob 迁移前资产完整存在 | SnailJob 2.0.2 依赖、`@EnableSnailJob` 自动配置、独立 server、8 个 `@JobExecutor`、前端 iframe 均存在；XXL-JOB 代码依赖不存在 | 直接新增 XXL-JOB 会形成调度双栈/重复触发风险 | 先冻结 SnailJob，审批迁移顺序、停止/回滚和单调度器规则；本轮不加依赖 | 是 |
| P0-004 | P0 | 正式契约和 Flyway 入口缺失 | `contract/*`、`db/migration`、`BASELINE.md` 均不存在 | 无法以契约优先、可重复迁移方式实现 API/数据库 | 先提交 OpenAPI/Schema/错误码/菜单事实源和 V1.x migration 设计 | 是 |
| P0-005 | P0 | 敏感配置存在前端暴露风险 | `plus-ui/.env.development`、`.env.production` 存在 RSA private-key 配置键；后端有密码/Token 类配置键 | 私钥可能进入前端构建产物；凭据可能被误提交/扩散 | 人工确认并执行轮换/撤销、删除前端私钥、改为安全注入；审计文档不输出值 | 是 |
| P0-006 | P0 | 数据库历史 SQL 不能直接作为 Flyway | V25 core SQL 使用 `information_schema` + `PREPARE/EXECUTE` 动态 `ALTER TABLE`；V24/V25 重复 10 个表名 | 非幂等/不可审计/跨环境行为不稳定，可能破坏生产迁移 | 隔离 legacy，拆成静态、版本化、可验证的 V1.x migration；不连接生产库 | 是 |
| P1-001 | P1 | 前端包管理器未冻结 | packageManager=`pnpm@10.34.5`，实测 pnpm=11.19.0；Node=25.2.1 | lockfile 安装/构建可出现不可重复差异 | 在 BASELINE 中冻结 Node/pnpm，并用声明版本执行 | 否（但阻塞前端验收） |
| P1-002 | P1 | 原型 API 与 V28 正式 API 不一致 | 原型 50 页/49 个去重 API 单元格；V28-2.1 29 个 operation；路径/方法/范围均有差异 | 直接照原型生成 API 会制造重复/过时接口 | 建立逐 operation 映射、兼容别名、deprecated 和删除计划 | 否（业务前置） |
| P1-003 | P1 | 原型调度说明与底座事实冲突 | 原型 README 写 XXL-JOB 唯一调度；底座实际仍是 SnailJob | 文档误导下一位实施模型 | 以底座实测和 V28 调度迁移门禁为准，更新前先审批 | 否 |
| P1-004 | P1 | SnailJob 示例任务未清理 | `ruoyi-modules/ruoyi-job` 存在 8 个 `@JobExecutor` 命中及相关示例类 | 迁移/删除不当可能导致任务丢失或重复 | 建立任务盘点和逐任务迁移/停用清单 | 否（迁移阶段阻塞） |
| P2-001 | P2 | 原型资源压缩/单行，审计可读性差 | `assets/app.js` 约 54 KB/94 行；`assets/styles.css` 约 18 KB/1 行 | 人工映射成本高，误读风险上升 | 仅作为视觉参考，不直接复制实现 | 否 |
| P2-002 | P2 | 菜单仍以运行时数据库接口为事实源 | `permission.ts` 调用 `/system/menu/getRouters`；正式菜单 YAML 不存在 | 契约/权限/前端路由可能漂移 | 后续建立菜单事实源和校验 CI | 否 |
| P2-003 | P2 | 多数据库 SQL 入口未统一 | 后端保留 MySQL/Oracle/PostgreSQL/SQL Server 脚本，尚无 QuantPF 单一 Flyway 入口 | Phase 0 迁移验证范围不明确 | 明确 Phase 0 目标数据库和兼容策略，其他脚本留底座 legacy | 否 |
