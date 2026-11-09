| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
| -- | -- | -- | -- | -- | -- | ---- |
| P0-001 | P0 | 根 POM 接入 QuantPF 会改变 40 工程 Reactor | 当前根 modules 不含 quantpf-modules；计划需修改根 pom.xml | 全仓构建顺序、依赖解析和发布边界变化 | Phase 0C 只按 manifest 修改，先 validate/依赖树，再人工批准 | 是 |
| P0-002 | P0 | 模块职责与依赖边界可能被历史 V25 内容污染 | V25 历史章节存在旧模块名和旧包结构；V28 明确以 quantpf-domain 持有 Entity/Mapper/XML | 循环依赖、Controller 下沉、Mapper 越权 | 以 V29/V28 正式规则为准，Phase 0C 加依赖扫描和包路径扫描 | 是 |
| P0-003 | P0 | SnailJob 迁移可能造成重复触发 | 当前有 SnailJob 依赖、自动配置、8 个 @JobExecutor、服务端、应用配置、前端页面 | 重复订单/账务/审计副作用 | 先逐任务盘点、幂等验证、切换窗口单调度器运行；本轮不迁移 | 是 |
| P0-004 | P0 | XXL-JOB 依赖版本/API 尚未在代码仓验证 | 当前没有 XXL-JOB 依赖或 Executor | 直接照搬旧示例会导致编译/运行时错误 | Phase 0C 前先冻结 3.1.0 具体 artifact/API；独立测试 Handler | 是 |
| P0-005 | P0 | Flyway 迁移与历史 SQL 表名冲突 | V23/V24/V25 SQL 有重复表名和动态 DDL | schema drift、不可重复迁移 | 只创建静态版本化 migration；先做表映射、静态扫描和 MySQL 8 隔离验证 | 是 |
| P0-006 | P0 | 正式契约缺失且原型 API 不一致 | 原型 49 个去重 API 单元格；V28 正式 29 operation | 前后端接口漂移和重复 API | 先固定 quantpf-v29 OpenAPI、映射矩阵、deprecated 策略 | 是 |
| P0-007 | P0 | 前端私钥配置仍在客户端边界 | .env.development/.env.production 存在 RSA private-key 配置键 | 私钥进入构建产物，导致安全事故 | Phase 0F 前完成人工批准的轮换/撤销和安全注入 | 是 |
| P0-008 | P0 | Phase 0C 计划文件遗漏会导致越权修改 | 当前允许修改范围只有文档，未来需新增 POM/目录/代码 | 实施模型自由发挥、污染底座 | 使用 manifest 白名单；未列文件禁止修改 | 是 |
| P1-001 | P1 | `ruoyi-admin` 一次性引入过多业务模块 | V28 仅建议最小 job/execution/health 接入 | 启动扫描扩大、边界不清 | Phase 0C 先只接入最小健康入口，业务闭环后续分阶段 | 否 |
| P1-002 | P1 | `quantpf-job` 间接获得 Mapper 或数据库权限 | DAG 中 job 依赖多个业务模块；domain 为 persistence owner | 调度层绕过 application service | 依赖树、架构规则和源码扫描三重门禁 | 否 |
| P1-003 | P1 | OpenAPI 文件名从 V28 `quantpf-v28.yaml` 漂移到 V29 `quantpf-v29.yaml` | V29 计划新增版本；V28 正式文件为 v28 | 生成脚本、引用和兼容路径不一致 | Phase 0C 前人工冻结版本策略；旧 v28 只作历史 reference | 否 |
| P1-004 | P1 | Node v25.2.1 不是 LTS | 当前 Node v25.2.1，pnpm 已对齐 | CI 可复现性和后续维护风险 | Phase 0C CI 计划中评估 Node LTS；本轮不擅自降级 | 否 |
| P1-005 | P1 | 原型扩展页面超出 29 operation | 原型包含 broker/data/ops 扩展接口；正式闭环范围更小 | 业务范围膨胀 | 标记 deferred/reference-only，不直接实现 | 否 |
| P2-001 | P2 | 原型 JS/CSS 压缩导致映射审查成本高 | app.js 约 54KB/94 行，styles.css 单行 | 人工审查误读 | 只作字段和交互参考，不复制实现 | 否 |
| P2-002 | P2 | 底座保留 SnailAI/多数据库脚本增加审查面 | 当前 `ruoyi-snailai-server` 与多数据库 SQL 仍在 | 施工误触发非目标资产 | manifest 明确 review-only，按阶段隔离 | 否 |
