# OpenSpec 产物 → Superpowers 技能 落地代码仓操作手册

> 适用仓库：`D:\workspace\quant`（QuantPF）
> 技能库：superpowers v7.0.1（Codex 与 Claude Code 双侧已装，2026-10-27 实测验证）
> 整理日期：2026-10-28 · 全部结论基于本机实测，非文档推测

---

## 0. 一页速查

**链路（5 条消息，按序粘贴；第 ③ 步二选一）：**

| # | 技能 | 干什么 | 提示词见 |
|---|---|---|---|
| ① | `superpowers:writing-plans` | 把 openspec 四件套翻译成可执行 plan | §3 |
| ② | `superpowers:using-git-worktrees` | 建隔离工作区 + `feat/<CHANGE>` 分支 | §4 |
| ③ | `superpowers:subagent-driven-development`（SDD）**或** `superpowers:executing-plans`（inline） | 逐 task 写代码 | §5 |
| ④ | `superpowers:finishing-a-development-branch` | 分支整合决策 | §6 |
| ⑤ | `/openspec-verify-change` → `/openspec-archive-change` | 回到 openspec 闭环 | §7 |

**执行循环内自动触发、无需手动喊的子技能：**
`test-driven-development`（每 task 红绿循环）、`systematic-debugging`（测试失败时）、
`verification-before-completion`（宣称完成前）、`requesting-code-review` / `receiving-code-review`（任务门禁与终审）。

**两个调用语法差异：**
- Codex：消息里直接写「使用 superpowers:xxx 技能 …」
- Claude Code：斜杠命令 `/superpowers:xxx`

---

## 1. 核心概念：为什么 tasks.md 不能直接执行

openspec-propose 产出的 `tasks.md` 是**阶段级**清单（本仓库实测样例）：

```markdown
- [ ] **1. 数据库**：[后端]/db/migration/ 下创建模块一 MySQL 建表/兼容迁移脚本……
- [ ] **2. 后端实体**：……验证实体与 DDL 映射、雪花 ID、五日志字段自动填充……
```

而 superpowers `writing-plans` 对 task 的定义是**最小可测试单元**（技能原文）：

> "Write the failing test" - step / "Run it to make sure it fails" - step /
> "Implement the minimal code to make the test pass" - step /
> "Run the tests and make sure they pass" - step / "Commit" - step

**直接把 tasks.md 喂给执行技能 = 7 个无法门禁的巨型 task，TDD 循环无从谈起。**
所以必须先经 ① 翻译。

### 产物映射

```
openspec-propose 产物                superpowers 技能                  产出
─────────────────────────────────────────────────────────────────────────────
proposal.md ─┐
design.md    ├─→ ① writing-plans        doc/superpowers/plans/
tasks.md     │                          YYYY-MM-DD-<CHANGE>.md
specs/      ─┘                          (含 Global Constraints + 细粒度任务)
                 ② using-git-worktrees   隔离 worktree + feat/<CHANGE> 分支
                 ③ SDD 或 inline         逐 task: implementer → TDD → reviewer
                 ④ finishing-branch      合并/PR 决策
                 ⑤ openspec verify/archive                        (回到 openspec)
```

---

## 2. 前置条件检查清单（本机已实测）

| 项 | 状态 | 证据 |
|---|---|---|
| Codex 侧 superpowers 技能 | ✅ 14/15 在 `~/.codex/skills`（CCSwitch 已纳管，`local` 源，claude=1 codex=1） | cc-switch.db skills 表 32 行 |
| Claude Code 侧 | ✅ 插件 `superpowers@superpowers-dev` v7.0.1 enabled | `~/.claude/plugins/installed_plugins.json` |
| 多智能体工具（SDD 门槛） | ✅ **新会话**有 V1 工具集 | `codex exec` 实测自报：`multi_agent_v1__spawn_agent / wait_agent / send_input / resume_agent / close_agent` |
| `config.toml` 开关 | ✅ `[features] multi_agent = true`（第 51-52 行） | 2026-10-27 写入，TOML 解析 + 类型校验通过 |
| 模型目录 multi_agent 档位 | ⚠️ 全部 16 个模型 = **v1**（CCSwitch 代理目录声明） | `cc-switch-model-catalog.json` |
| Git bash（SDD 脚本依赖） | ✅ `C:\Program Files\Git\bin\bash.exe` | 脚本实测可执行，CRLF 无影响 |
| 计划保存目录 | ⚠️ 本仓库是 `doc/` 不是 `docs/`，指令中须显式覆盖技能默认路径 | 实测 `Test-Path doc=True, docs=False` |

**两个必须记住的约束：**

1. **老会话没有多智能体工具。** 工具集在会话启动时快照。本手册撰写时的 thread 创建于 `multi_agent=true` 之前，故无工具；**新开会话即有**。
2. **V1 与 V2 的两处差异**（superpowers `codex-tools.md` 原文对应）：
   - 修复轮：V2 用 `followup_task`；**V1 用 `send_input` / `resume_agent`**
   - 生命周期：V2 子智能体自动驱逐；**V1 必须手动 `close_agent`**（reviewer 返回后关、implementer 评审通过后关）

---

## 3. 步骤① writing-plans —— 翻译 spec 为可执行 plan

**Codex 提示词（复制后替换 `<CHANGE>`）：**

```text
使用 superpowers:writing-plans 技能，把 openspec/changes/<CHANGE>/ 落地为实施计划。
Spec 来源（按权威性排序）：specs/ 目录、design.md、proposal.md、tasks.md。
计划保存到 doc/superpowers/plans/2026-10-28-<CHANGE>.md（本仓库惯例是 doc/ 不是 docs/，以此覆盖技能默认路径）。
要求：
- tasks.md 的阶段只是阶段划分，必须按技能粒度拆成"红测试→绿测试→提交"的细粒度 task；
- design.md 里的全局硬约束（雪花 BIGINT 主键、禁 AUTO_INCREMENT、五日志字段、JDK21、Dify v1.16.1 等）逐字抄入计划的 Global Constraints 段；
- 计划头部 Spec 字段指向 openspec/changes/<CHANGE>/design.md。
```

**Claude Code 等价：** `/superpowers:writing-plans` + 同一段要求。

**产出文档的强制头部**（技能规定格式，也是执行技能的入口约定）：

```markdown
# [Feature Name] Implementation Plan
> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task.
**Spec:** openspec/changes/<CHANGE>/design.md
```

**Scope Check 提醒：** `*-integrated-delivery` 这类整合 change 覆盖多子系统，技能会建议拆成多个 plan——听它的，每个 plan 独立可测试。

---

## 4. 步骤② using-git-worktrees —— 隔离工作区

```text
使用 superpowers:using-git-worktrees 技能，为计划 doc/superpowers/plans/2026-10-28-<CHANGE>.md
创建隔离工作区，分支名 feat/<CHANGE>。
```

**本仓库两个现实约束（实测）：**

- 主工作区当前是**脏的**（`doc/` 有未提交修改、`RuoYi-Vue-Plus` 子模块有改动）。worktree 从 HEAD 检出，未提交内容**不会跟过去**；若实现依赖它们，先提交或 stash。
- 技能 Step 0 先跑 `GIT_DIR != GIT_COMMON` 检测是否已在 worktree；Codex App 沙箱若拒绝 `git worktree add`，技能自带降级：原地工作并告知。

---

## 5. 步骤③ 执行 —— SDD 与 inline 二选一

**判定标准（唯一门槛）：会话工具列表里有没有 `spawn_agent` 系工具。**

| 条件 | 选择 |
|---|---|
| 有 `multi_agent_v1__spawn_agent` / `wait_agent` | **SDD**（推荐：每 task 独立实现+评审双门禁） |
| 没有 | **inline**（本会话自己写，结尾一次整分支终审） |

懒人法：先贴 SDD；技能自带自检（"Never fabricate a dispatch"），无派发工具时会明确让你改 inline。

### 5A. SDD 提示词

```text
使用 superpowers:subagent-driven-development 执行计划 doc/superpowers/plans/2026-10-28-<CHANGE>.md，
逐 task 派发 implementer 与 reviewer。ledger 记在技能工作区内。
模型分档按技能 Model Selection 一节执行：机械实现用最便宜档，终审用最强档，派发时必须显式指定 model。
```

**SDD 内部自动调用的脚本**（无需手动执行，排错时认得即可）：

```bash
bash scripts/sdd-workspace PLAN_FILE              # 建本 plan 专属工作区 <repo>/.superpowers/sdd/<plan>/
bash scripts/task-brief  PLAN_FILE N              # 提取第 N 个 task 全文给 implementer
bash scripts/task-start  PLAN_FILE N              # 记录 BASE commit
bash scripts/task-done   PLAN_FILE N BASE -- <测试命令>
bash scripts/review-package PLAN_FILE BASE HEAD   # 打包给 reviewer
```

> ⚠️ `.superpowers/sdd/` 是 git-ignored 的 scratch 目录，**`git clean -fdx` 会毁掉 ledger**；真毁了按技能说明从 `git log` 恢复。

**V1 专属注意**：reviewer 返回后 `close_agent` 关 reviewer；implementer 的 review 通过后 `close_agent` 关 implementer。修复轮用 `send_input` / `resume_agent` 而非 V2 的 `followup_task`。

### 5B. inline 提示词

```text
使用 superpowers:executing-plans 在当前会话内联执行计划 doc/superpowers/plans/2026-10-28-<CHANGE>.md。
任务之间不要停下来问我；冲突按"spec 是权威、plan 是论证、你的判断裁决"处理，
每条裁决以 "Ruling: 决定了什么 — 为什么 — 错了代价是什么" 记入 ledger。
```

### 模型分档建议（对应你的 CCSwitch 目录 slug）

| 角色 | 建议档位 |
|---|---|
| 机械实现（plan 文本含完整代码 = 转录+测试） | `ccs-p836b4e/glm-5.3-flash` / `ccs-p836b4e/deepseek-v4.1-flash` |
| 集成/判断型实现、评审 | `ccs-p836b4e/glm-5.3` / `ccs-p836b4e/kimi-k3` |
| 架构、整分支终审 | `ccs-p836b4e/qwen3.8-max` |

> 派发时**必须显式写 model**；不写会继承主会话模型（通常最贵档），技能原文称这是"悄悄烧钱"的头号陷阱。

---

## 6. 步骤④ finishing-a-development-branch

```text
使用 superpowers:finishing-a-development-branch 决定 feat/<CHANGE> 分支的整合方式。
```

---

## 7. 步骤⑤ 回到 openspec 闭环

```text
/openspec-verify-change     # 核对实现与 specs/ 的 WHEN/THEN
/openspec-archive-change    # 同步 capability specs 并归档
```

（与你各 `tasks.md` 末尾 "Workflow follow-up" 一节的要求一致。）

---

## 8. 完整示例：research-candidate-db-design

```text
① 使用 superpowers:writing-plans 技能，把 openspec/changes/research-candidate-db-design/
   落地为实施计划，保存到 doc/superpowers/plans/2026-10-28-research-candidate-db-design.md，
   Spec 指向该 change 的 design.md，全局约束逐字抄入 Global Constraints。

② 使用 superpowers:using-git-worktrees 技能创建隔离工作区，分支 feat/research-candidate-db-design。

③ 使用 superpowers:subagent-driven-development 执行计划
   doc/superpowers/plans/2026-10-28-research-candidate-db-design.md（若无多智能体工具则改用
   superpowers:executing-plans 内联执行）。

④ 使用 superpowers:finishing-a-development-branch 整合分支。

⑤ /openspec-verify-change 然后 /openspec-archive-change。
```

---

## 9. 避坑清单

1. **别跳过①**：tasks.md 直喂执行技能 = 巨型 task 无门禁。
2. **plan 路径**：技能默认 `docs/superpowers/plans/`，本仓库只有 `doc/`；技能允许用户偏好覆盖，指令中已显式声明。
3. **一个 change 一个 plan**：整合型 change 先按 Scope Check 拆分。
4. **老会话无多智能体工具**：开新会话再跑 SDD。
5. **V1 记得 close_agent**：不关会占槽位（V1 无自动驱逐）。
6. **Claude Code 语法**：插件技能带命名空间，写 `/superpowers:writing-plans`；Codex 侧消息里点名即可。
7. **终审别省**：inline 模式下整分支终审是唯一的第二双眼睛，用最强档。
8. **worktree 前处理脏工作区**：未提交改动不会跟进 worktree。
9. **别 `git clean -fdx`**：会毁 SDD 的 git-ignored ledger。

---

## 附录 A：本机 superpowers 技能清单（15 个）

brainstorming · diagnosing-superpowers · dispatching-parallel-agents · executing-plans ·
finishing-a-development-branch · receiving-code-review · requesting-code-review ·
subagent-driven-development · systematic-debugging · test-driven-development ·
using-git-worktrees · using-superpowers · verification-before-completion ·
writing-plans · writing-skills

（Codex 侧 `brainstorming` 曾为试点空出；若缺失，从备份
`C:\Users\78135\.codex\skills-superpowers-backup-20261027_171714\` 恢复，
或在 CCSwitch 的 skills.sh 页签安装 `obra/superpowers/brainstorming`。）

## 附录 B：验证与回滚工具

```powershell
node C:\Users\78135\.codex\verify-pilot.cjs          # 以 GitHub zip 为基准逐字节校验托管完整性
powershell -File C:\Users\78135\.codex\restore-superpowers.ps1   # 从备份恢复全部 15 技能
```

## 附录 C：关键实测结论存档

- CCSwitch 能完整托管多文件技能：SSOT 中 14 个 superpowers 技能 76/76 文件与权威 zip 逐字节一致（2026-10-27）。
- CRLF 对 Git Bash 执行无影响：5 个 bash 脚本 CRLF/LF 版输出与退出码逐一相同（2026-10-27 实测）。
- `multi_agent = true` 被 Codex 真实解析：传非法类型报 `invalid type ... in features`（2026-10-27）。
- V1 工具集存在性：`codex exec` 会话自报 5 个 `multi_agent_v1__*` 工具（2026-10-28）。
- 日志中 `feature.multi_agent_v2=false` 是**预期值**（目录声明 v1），不可当作"未启用"证据。
