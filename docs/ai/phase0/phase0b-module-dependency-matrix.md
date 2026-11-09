# QuantPF Phase 0B Module Dependency Matrix

- 日期：2026-10-03
- `planOnly`: `true`
- 本文件只描述计划，不代表已创建模块或已修改 POM。

## 1. Target DAG

```text
ruoyi-admin
 ├── quantpf-job
 ├── quantpf-execution
 ├── quantpf-backtest
 ├── quantpf-research
 ├── quantpf-finance
 ├── quantpf-marketdata
 └── ruoyi-common-*

quantpf-job        → quantpf-execution, quantpf-research, quantpf-marketdata, quantpf-backtest
quantpf-backtest   → quantpf-strategy, quantpf-risk, quantpf-domain, quantpf-common
quantpf-execution  → quantpf-risk, quantpf-domain, quantpf-common
quantpf-finance    → quantpf-research, quantpf-domain, quantpf-common
quantpf-research   → quantpf-marketdata, quantpf-domain, quantpf-common
quantpf-strategy   → quantpf-domain, quantpf-common
quantpf-risk       → quantpf-domain, quantpf-common
quantpf-marketdata → quantpf-domain, quantpf-common
quantpf-domain     → quantpf-common, RuoYi common persistence modules
```

## 2. Matrix

| Module | Direct QuantPF deps | RuoYi deps | Persistence | External side effects | Forbidden |
|---|---|---|---|---|---|
| quantpf-common | none | only approved neutral common APIs | none | none | Entity/Mapper/XML/Web/Job/Broker |
| quantpf-domain | common | common persistence modules | owner | none | Web/Job/Broker |
| quantpf-marketdata | domain, common | neutral web/json/persistence as approved | via domain | read-only providers | order/execution |
| quantpf-research | marketdata, domain, common | approved common | via domain | provider calls only through ports | order/Broker |
| quantpf-strategy | domain, common | approved common | via domain | deterministic calculation | direct execution |
| quantpf-risk | domain, common | approved common | via domain | risk state/application events | direct SQL/Broker |
| quantpf-finance | research, domain, common | approved common | via domain | research/analysis providers | order/submit |
| quantpf-execution | risk, domain, common | approved common | via domain | PaperBrokerAdapter only | QMT/CTP/LIVE |
| quantpf-backtest | strategy, risk, domain, common | approved common | via domain | isolated backtest execution | production order |
| quantpf-job | execution, research, marketdata, backtest | scheduler adapter only | via application services | scheduler trigger | Mapper/SQL/Shell/Broker |

## 3. Layer rules

- `quantpf-common`: pure shared contracts and value types.
- `quantpf-domain`: sole persistence owner for QuantPF entities/mappers/XML/repositories.
- Application/use-case services stay with their owning business module.
- Controllers stay only in `ruoyi-admin/src/main/java/com/quantpf/web/controller`.
- No module may introduce `org.dromara` QuantPF packages.
- No module may register live execution capability.

## 4. Cycle checks for Phase 0C

1. Maven dependency tree must not contain a cycle.
2. `quantpf-common` must have no business-module dependency.
3. `quantpf-domain` must not depend on admin/web/job.
4. `quantpf-job` must not have a direct or transitive mapper implementation dependency without explicit architecture approval.
5. `quantpf-execution` must not contain QMT/CTP SDK or live submit/cancel bean.
6. `ruoyi-admin` may depend on application-facing modules only; it may not call mapper classes directly.

## 5. Planned validation

```powershell
mvn -DskipTests=true validate
mvn -DskipTests dependency:tree
```

Then perform source scans for:

```text
com.quantpf.web.controller outside ruoyi-admin
@LiveEnabled
qmtSubmitEnabled
ctpSubmitEnabled
LiveOrderGateway as Spring Bean
Mapper usage from quantpf-job
SQL construction from quantpf-job
```
