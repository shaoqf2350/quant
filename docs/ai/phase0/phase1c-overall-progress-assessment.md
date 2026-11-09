# QuantPF 当前整体完成进度评估

- 评估日期：2026-10-07
- 评估范围：Phase 0 至 Phase 1C Contract Closure
- 评估口径：按“基本可交付源代码工程”能力门禁，不按代码行数

| 范围 | 状态/估算 | 说明 |
|---|---:|---|
| Phase 0 基础、模块骨架、隔离 Web、契约/Flyway 静态文件 | 85%～90% | 底座和静态契约已建立；正式发布门禁、调度迁移及部分事实核验仍未完成 |
| Phase 1A Paper 领域与 Application API | 100% | 领域、状态机、风控闸门、Paper API 和隔离测试已完成 |
| Phase 1B Paper 后端 | 86%～90% | 账户/订单/风险、事务并发、订单生命周期、Audit/Job、Follow 持久化已有较完整实现；历史 handoff 仍有部分闭环需最终统一验收 |
| Phase 1C 前端契约与安全闭合 | 约 30% | 只读盘点、差异矩阵、白名单和风险门禁完成；实际前端页面/API wrapper 尚未实施 |
| Phase 1C 前端页面实现 | 0% | 因客户端私钥、DTO/分页、菜单 DB、网关路由等门禁仍未关闭，明确禁止实施 |
| 面向基本可交付源代码工程的整体进度 | **约 76%（合理区间 73%～80%）** | 与上一阶段审计基线一致；不能宣称已达到可交付产品或生产部署状态 |

## 当前可以交付

- Paper 后端核心源码的大部分；
- 持久化适配、事务、并发幂等和生命周期测试；
- OpenAPI/错误码/菜单事实源的静态基础；
- 前端契约审计、延期清单和候选白名单。

## 当前不能交付

- 完整 Paper 前端页面；
- 已通过安全门禁的前端生产构建；
- 已核验的生产网关路由和 RuoYi `sys_menu` 数据库事实；
- 完整生产部署包；
- LIVE/QMT/CTP/Broker 能力；
- SnailJob→XXL-JOB 迁移后的生产调度能力。

## 继续提升至基本可交付所需的最短路径

1. 完成客户端私钥轮换/撤销、清理、后端加密兼容和脱敏 secrets scan；
2. 冻结 Paper 12 个已实现端点的 DTO、分页/列表、HTTP/body 错误和幂等契约；
3. 对 10 个未实现 OpenAPI path 做明确延期/移除/排期；
4. 核验正确 RuoYi schema 的 `sys_menu.component/perms` 和生产网关 `/api/quant` 路由；
5. 获批后只实现 Account/Ledger、Order list/submit、Risk status、Audit read-only；
6. 完成前端 lint、受控构建和人工验收；
7. 最后再统一更新整体交付判定。
