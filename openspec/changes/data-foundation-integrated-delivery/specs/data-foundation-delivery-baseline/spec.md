# Spec Delta

## Purpose

定义模块一数据库、Spring Boot、Vue 和 Dify 的一体化交付基线，确保跨仓实现遵守统一的数据主库、Dify 边界、ID/日志字段、一期资产与 Paper 隔离原则，并能按契约验收。

## ADDED Requirements

### Requirement: 跨仓实施必须遵守统一权威边界
后端、前端和数据库实现 SHALL 以 MySQL 业务主库、DuckDB K 线/Tick、Spring Boot 事实源、Dify 草稿层和 RuoYi 权限事实源为唯一边界；模块一不得直接创建 Paper 订单或真实交易能力。

#### Scenario: 代码提交包含跨域直写
- **WHEN** 模块一代码尝试从 Controller/Dify/采集任务直接写 Paper 订单、资金、持仓或风险状态
- **THEN** 架构检查或代码评审拒绝该实现，并要求通过模块四契约/事件交接

#### Scenario: 前端请求 Dify
- **WHEN** 浏览器尝试携带 Dify Key 访问 Dify API
- **THEN** 请求被架构/网关边界拒绝，前端只能调用 Spring Boot 研究接口

### Requirement: 数据基础用户路径可恢复
系统 SHALL 支持管理员配置数据源、审核用户处理冲突、普通用户查看数据就绪并创建研究任务；任一步骤失败、超时或页面离开后，状态、版本、幂等键和下一步必须可恢复。

#### Scenario: 普通用户创建研究任务
- **WHEN** 用户选择对象、问题和周期后提交
- **THEN** 任务保存输入快照和幂等键，页面显示准备/排队/分析/核验状态，不要求用户填写接口、指标或风险参数

#### Scenario: 数据问题修复后恢复
- **WHEN** 管理员解决影响研究的质量问题
- **THEN** 原 GuidedTask/研究任务保留已确认输入，并从数据资格重新满足后的步骤继续

### Requirement: 数据库设计、代码目录和 UI 契约必须一致
数据库表、后端实体/接口、前端 API/页面和 Dify 草稿字段 SHALL 使用同一业务对象、状态、版本和权限语义；任何不一致必须在契约或设计审查中被发现。

#### Scenario: 研究任务字段对拍
- **WHEN** 后端任务 DTO、数据库研究任务表和前端任务类型完成构建
- **THEN** task ID、状态、snapshot/as-of、授权/质量、Dify run 和错误字段可相互映射，缺失或类型不一致使契约检查失败

### Requirement: 一期边界和联调门禁可验证
实现 SHALL 明确一期仅 A 股/国内期货、日线主流程、施工期 Mock、Paper/Shadow 隔离和真实交易禁用；联调必须在 JDK21 + Maven 后端和 pnpm 前端启动后执行。

#### Scenario: 一期页面扫描
- **WHEN** 对菜单、路由、API 和构建产物执行能力扫描
- **THEN** 不存在分钟/Tick 盘中交易、LIVE、QMT、CTP、真实 Broker submit/cancel 能力

#### Scenario: 前后端联调
- **WHEN** 后端以 JDK21 + Maven 启动 `DromaraApplication`，前端执行 `pnpm dev`
- **THEN** 用户可以从登录后的量化工作台完成数据状态查看、研究任务创建和 Dify Mock/降级状态验证，且 Paper 现有回归测试继续通过
