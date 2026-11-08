# Spec Delta

## Purpose

定义 QuantPF 模块一行情与衍生数据域的持久化行为契约：K 线与 Tick 的 DuckDB 时序存储边界（ADR-9）、MySQL 盘口摘要与字段级降级、市场状态、容量评估、量价特征快照、商品供需、基差/期限结构与现货报价，保证行情数据口径一致、可回放、可为策略/风险/回测/Paper 提供合格输入。

## ADDED Requirements

### Requirement: K 线与 Tick 的存储权威边界
系统 SHALL 以嵌入式 DuckDB 作为 K 线与 Tick 数据的唯一权威存储，K 线周期集合限定为 30 分钟、日、周、月（一期不含 1/5/15/60 分钟线）；其余全部数据（含盘口摘要、因子、新闻/财报概要及分析结果）MUST 统一存 MySQL。既有 MySQL K 线表 SHALL 冻结为兼容只读，新采集 MUST NOT 写入。一期业务采集与消费以日线为主，30 分钟线与 Tick 仅建结构预留。

#### Scenario: 日线采集入库
- **WHEN** 交易日盘后日线采集完成并通过质量校验
- **THEN** K 线写入 DuckDB（含复权口径、来源、导入批次），MySQL 旧 K 线表无新写入

#### Scenario: DuckDB 数据文件持久化
- **WHEN** 应用容器重启
- **THEN** DuckDB 数据文件经挂载卷保留，schema 版本管理器幂等校验结构版本，不重复建表、不丢历史数据

### Requirement: K 线合法性与自然键去重
系统 SHALL 在 K 线持久化前后执行合法性校验（high≥low、high≥open/close、low≤open/close、成交量额非负、时间属于有效交易时段、交易日属于交易日历、标的身份有效），并以（标的，周期，K 线时间，复权口径，来源）作为唯一自然键去重；导入 MUST 支持 FULL/INCREMENTAL/REPAIR 三种模式且重复导入不产生重复行。

#### Scenario: 非法 K 线拒收
- **WHEN** 采集到的 K 线 high < low
- **THEN** 该行进入质量问题记录而不写入 K 线存储，来源与批次可追溯

#### Scenario: 增量导入幂等
- **WHEN** 同一交易日日线以 INCREMENTAL 模式重复导入
- **THEN** 按自然键去重，行数不变，导入批次记录更新

### Requirement: 盘口摘要存 MySQL 并支持字段级降级
系统 SHALL 将盘口摘要（含开收高低、成交量额、量比 Z、换手 Z、换手率、外盘、内盘、委比、买卖价差、盘口深度、涨跌停价、估值与股本市值字段）持久化到 MySQL，并逐字段记录可得性；依赖实时类接口的字段（外盘、内盘、委比、量比、Tick）不可得时 MUST 标记 `PARTIAL`/`DELAYED` 并限制下游用途，MUST NOT 阻塞日线主链路。

#### Scenario: 实时字段不可得
- **WHEN** 联调后发现某实时盘口字段无权限
- **THEN** 字段可得性标记为不可得，盘口摘要质量状态为 PARTIAL，依赖该字段的功能自动降级，日线+30 分钟 K 线主链路不受影响

### Requirement: 市场状态由确定性规则产生并可回放
系统 SHALL 按作用域（市场/指数/行业/标的/品种）与交易日持久化市场状态（TREND_UP/TREND_DOWN/RANGE/HIGH_VOLATILITY/LOW_LIQUIDITY/SYSTEMIC_RISK/DATA_UNAVAILABLE/SPECIAL_RULE），并保存计算输入快照引用与规则版本；市场状态 MUST 由 Spring Boot 确定性规则计算，AI 输出 MUST NOT 写入该状态。

#### Scenario: 状态重算可回放
- **WHEN** 以相同输入快照与规则版本重算某日市场状态
- **THEN** 结果与已持久化状态一致，差异可定位到输入或规则版本变化

### Requirement: 容量评估状态门禁
系统 SHALL 按标的与交易日持久化容量评估（SUFFICIENT/LIMITED/HIGH_IMPACT/UNTRADEABLE/UNKNOWN）及其输入指标与规则版本；`UNTRADEABLE` 与 `UNKNOWN` MUST 阻断新开仓计划生成，`LIMITED` MUST 限制风险规模，`HIGH_IMPACT` 仅允许观察或低规模 Paper。

#### Scenario: 容量未知阻断
- **WHEN** 某标的容量评估为 UNKNOWN
- **THEN** 下游交易计划资格校验读取该状态并拒绝新开仓，拒绝原因可追溯到评估记录

### Requirement: 量价特征快照与二期预留
系统 SHALL 持久化量价特征快照，含特征定义版本、特征值集合、关联市场状态、点时版本引用与来源快照引用；一期仅保存确定性基础特征与状态，二期交易时机优化 MUST 能基于特征版本与点时引用回放，特征 MUST NOT 仅保存在向量库或 AI 侧。

#### Scenario: 特征版本升级
- **WHEN** 特征定义版本变更后重算
- **THEN** 新旧版本快照并存，各自关联当时的输入快照与点时版本，历史研究引用不被覆盖

### Requirement: 商品供需与基差期限结构点时化
系统 SHALL 持久化商品供需数据（库存、供应、需求、开工、利润、进出口、季节性）与基差/期限结构数据，包含统计口径（地区/品级/单位）、period_end、published_at、available_at、修订号与季节性分位；修订 MUST 保留旧版本；历史不完整的数据 MUST 标记完整性状态，MUST NOT 包装成完整样本用于概率证明。

#### Scenario: 库存数据修订
- **WHEN** 某库存统计值被来源修订
- **THEN** 修订号递增、旧版本保留并关联，依赖该值的研究结论被标记需复核

### Requirement: 现货报价仅作研究输入
系统 SHALL 持久化现货报价（商品、品级、地区、价格类型、报价时间、单位、币种、成交条件、来源与质量状态）；现货记录 MUST 区分观察价格与可交易工具，无现实敞口时 MUST NOT 由现货看空记录派生现货空单表达。

#### Scenario: 现货看空研究
- **WHEN** 现货报价走弱且用户无库存/合同敞口
- **THEN** 现货数据仅作为期货空头研究或回避输入，系统不生成无依据的现货空单对象
