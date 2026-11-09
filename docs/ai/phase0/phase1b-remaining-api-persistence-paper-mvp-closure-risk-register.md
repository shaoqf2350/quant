# QuantPF Phase 1B Remaining API Persistence and Paper MVP Closure - Risk Register

- 登记日期：2026-10-07
- 范围：Audit、Job execution、Follow schema 阻塞、Paper MVP 门禁
- 数据库范围：仅本地 `127.0.0.1:3306/quantpf`

| ID | 等级 | 风险 | 证据 | 影响 | 建议 | 是否阻塞 |
|---|---|---|---|---|---|---|
| P1B-RAP-P0-001 | P0 | Follow 没有正式数据库事实源 | 当前 `QuantPaperFollowController` 仍使用 `AtomicBoolean`；当前 schema 未发现 Follow session/state 表 | 重启丢失状态；多个实例状态不一致；无法审计 start/stop 生命周期 | 先人工批准 Follow schema、幂等和并发唯一性，再做独立 Flyway 施工 | 是，阻塞 Phase 1C |
| P1B-RAP-P1-001 | P1 | Audit API 原先固定返回空列表 | 本轮已接入 `quantpf_audit_record`；Mapper/XML、Repository、Service、Controller 链路已验证；DB fact 2/2 PASS | 原先无法展示审计事实 | 保持数据库只读链路和异常传播；新增写入时复用明确幂等边界 | 否，已解决 |
| P1B-RAP-P1-002 | P1 | Job execution API 原先固定返回空列表 | 本轮已接入 `quantpf_job_execution_audit`；过滤、排序、分页和 DB fact 2/2 PASS | 原先无法展示执行审计事实 | 仅由正式调度施工阶段增加写入；不要在本轮初始化任何调度器 | 否，已解决 |
| P1B-RAP-P1-003 | P1 | Follow 正式路径仍为内存状态 | `AtomicBoolean` 是当前唯一状态；本轮按 schema 边界未触碰 | Follow 不是可交付的多实例持久化能力 | 不能改名伪装；完成专用表、Repository、Application Service、事务和幂等测试 | 是，阻塞 Phase 1C |
| P1B-RAP-P1-004 | P1 | Follow/Audit/Job API 与数据库事实尚未完全统一 | Audit/Job 已数据库化，Follow 仍内存态 | Paper API 仍存在事实源不一致 | Follow 迁移完成后重跑完整 Paper MVP 回归 | 是，阻塞 Phase 1C |
| P1B-RAP-P1-005 | P1 | whitelist 对 Follow proposal 自相矛盾 | `migrationProposalRequired=true`，同时 proposal 路径在 `forbiddenFiles` | 无法在当前白名单内产出独立 proposal 文件 | 人工决定扩展白名单或接受 closure report 内的非执行 proposal | 是，阻塞 Follow 施工，不直接阻塞 Audit/Job |
| P1B-RAP-P1-006 | P1 | 独立 GLM-5.3 review artifact 缺失 | 计划引用的 `phase1b-remaining-api-persistence-independent-review.md` 不存在 | 无法引用可验证的独立模型签字记录 | 由后续审查模型生成独立 review，并与实现结果分离保存 | 否，本轮由等效静态审查覆盖 |
| P1B-RAP-P2-001 | P2 | Audit/Job 表缺少历史真实数据 | 本轮测试前事实行数为 0；测试使用随机 `RAP_` 前缀 | 只能证明读路径，不能证明生产数据质量 | 后续由正式审计写入路径产生数据，并增加 schema/contract 级数据质量测试 | 否 |
| P1B-RAP-P2-002 | P2 | 当前使用本地 `quantpf` schema 而不是专用测试 schema | 集成测试连接本地 schema，靠随机前缀和 scoped cleanup 隔离 | 并行测试可能发生数据干扰 | 后续采用专用测试 schema、Testcontainers 或显式测试租约 | 否，本轮可接受 |
| P1B-RAP-P2-003 | P2 | 调度器迁移尚未完成 | 本轮未连接 SnailJob/XXL-JOB，也未增加调度器写入 | Job execution 只有查询闭环，尚无迁移运行证据 | 单独进入调度迁移窗口，遵守单调度器原则和回滚边界 | 否，本阶段不施工 |
| P1B-RAP-P2-004 | P2 | Phase 1C 前端尚未实施 | Phase 1C 明确为 `NOT ALLOWED` | 暂不能形成完整前后端交付 | 先关闭 P0 Follow 和 Phase 0 交付门禁 | 是，当前阶段性门禁 |
| P1B-RAP-P2-005 | P2 | 生产级认证、权限、菜单和错误码联调尚未复验 | 本轮只做受控隔离 API 和数据库事实测试 | 不能把当前结果等同于生产部署验收 | 在前端和安全接入阶段补做契约/权限/错误码验收 | 否，本阶段外 |
