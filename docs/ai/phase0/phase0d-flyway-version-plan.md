# PHASE0D Flyway Version Plan

- Target DB: MySQL 8.x
- Logical database: `quantpf`
- Execution: not performed in this phase

| Version | Scope | Main tables/constraints | Risk |
|---|---|---|---|
| V1_0_0 | baseline | migration metadata and common conventions | naming drift |
| V1_1_0 | instrument/market | instrument, market snapshot, trading calendar | provider schema |
| V1_2_0 | research/signal | news, financial statement, factor, strategy signal | source freshness |
| V1_3_0 | strategy/backtest | strategy, backtest run, backtest result | large result volume |
| V1_4_0 | paper account/ledger | account, cash ledger, position | monetary precision |
| V1_5_0 | order/execution | order, execution, idempotency keys | state/duplicate writes |
| V1_6_0 | risk/halt | risk snapshot, halt record, risk rule | safety precedence |
| V1_7_0 | job/audit | job execution and audit records | scheduler migration |
| V1_8_0 | indexes/constraints | unique keys, indexes, check constraints | upgrade compatibility |

Prohibited SQL: `CREATE DATABASE`, `USE`, `DROP TABLE`, dynamic DDL, destructive rollback, and direct execution of historical SQL.

Historical QuantPF SQL remains legacy/reference-only.