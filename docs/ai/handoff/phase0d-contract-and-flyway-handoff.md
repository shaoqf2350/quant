# QuantPF Phase 0D Contract and Flyway Handoff

## 1. 当前目标

`PHASE0D-CONTRACT-AND-FLYWAY-PLAN`

## 2. 本轮状态

计划完成；正式 OpenAPI、事件 Schema、错误码、菜单 YAML 和 Flyway SQL 尚未创建。

## 3. Dev YAML 状态

已按用户授权更新：

```text
D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\resources\application-dev.yml
```

目标：本地 MySQL logical database `quantpf`，Redis port `6379`，Redis database `6`。本地密码未写入任何 Phase 0 文档，不在本交接文件中输出。

## 4. 正式契约计划

- OpenAPI：`contract/openapi/quantpf-v29.yaml`；
- Events：order/execution/ledger/risk v1；
- Error codes：`contract/error-codes/quantpf-error-codes.yaml`；
- Menu facts：`contract/menu/quantpf-menu-facts.yaml`。

## 5. Flyway计划

MySQL 8.x、逻辑数据库 `quantpf`，V1_0_0 至 V1_8_0，先计划后实施；本轮未创建 SQL、未连接数据库、未迁移。

## 6. 功能逻辑优先级

下一阶段功能实现优先顺序：

```text
Paper domain model
→ account/ledger
→ order state machine
→ PaperBrokerAdapter
→ risk gates
→ backtest boundary
→ application services
→ HTTP APIs
```

## 7. 当前未关闭 P0

- SnailJob 未迁移；
- 正式契约未创建；
- Flyway 未创建；
- 敏感配置轮换/撤销未闭环；
- 完整生产应用启动未验证。

## 8. 下一步目标

计划审批后：

```text
PHASE0D-CONTRACT-AND-FLYWAY-IMPLEMENTATION
```

随后：

```text
PHASE1A-PAPER-DOMAIN-IMPLEMENTATION
```

## 9. 推荐模型

契约/Flyway实施和边界复核：`GPT-5.6 Sol`。

Paper 业务代码按 manifest 批量实现：`deepseek-v4-pro`。

GLM-5.3 用于契约、SQL计划和风险登记二次审阅。

## 10. 未执行

未连接 MySQL、Redis、Broker；未执行 SQL/Flyway；未启动 SnailJob/XXL-JOB；未创建 QMT/CTP/LIVE；未提交 Git。