# QuantPF Phase 1B Spring Persistence/Application Cutover Handoff

## 1. 当前目标

`PHASE1B-SPRING-PERSISTENCE-INTEGRATION-AND-APPLICATION-CUTOVER`

## 2. 当前结果

```text
PHASE1B-SPRING-PERSISTENCE-INTEGRATION-AND-APPLICATION-CUTOVER: PARTIAL
PHASE1C-PAPER-FRONTEND-IMPLEMENTATION: NOT ALLOWED
```

## 3. 本轮已完成

- `com.quantpf.web` 受控扫描边界；
- `QuantPfPaperRuntimeConfiguration`；
- Spring DataSource/SqlSessionFactory/MapperScan/Bean wiring 隔离验证；
- account/order API 的持久化 Application Service 路径；
- Spring ApplicationContext + bean-backed MockMvc 测试；
- 本地 MySQL account/order 写读和成交事实验证；
- Health 和既有 standalone API 回归测试。

## 4. 实际关键文件

- `D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\org\dromara\DromaraApplication.java`
- `D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\com\quantpf\web\config\QuantPfPaperRuntimeConfiguration.java`
- `D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\com\quantpf\web\controller\QuantPaperAccountController.java`
- `D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\main\java\com\quantpf\web\controller\QuantPaperOrderController.java`
- `D:\workspace\quant\RuoYi-Vue-Plus\quantpf-modules\quantpf-finance\src\main\java\com\quantpf\finance\PersistentPaperAccountApplicationService.java`
- `D:\workspace\quant\RuoYi-Vue-Plus\quantpf-modules\quantpf-finance\src\main\java\com\quantpf\finance\PersistentPaperOrderApplicationService.java`
- `D:\workspace\quant\RuoYi-Vue-Plus\ruoyi-admin\src\test\java\com\quantpf\web\controller\QuantPfSpringPersistenceWebIntegrationTest.java`

## 5. 验收证据

```text
QuantPfSpringPersistenceWebIntegrationTest: 2 passed
QuantPfHealthControllerWebMvcTest: PASS
QuantPaperApiStandaloneTest: PASS
quantpf-domain/risk/execution/finance regression: PASS
ruoyi-admin -am compile: BUILD SUCCESS
```

只连接本地 `127.0.0.1:3306/quantpf`；凭据没有写入任何文件。

## 6. 当前 P0 阻塞项

### P0-001：完整 ruoyi-admin runtime 尚未安全启动验收

本轮使用受控 Spring ApplicationContext 和 bean-backed standalone MockMvc，而不是启动完整 admin WebApplicationContext/HTTP 服务。dynamic-datasource、Redis、SnailJob 等生产自动配置未被触发验证。

### P0-002：完整 Paper API 尚未全部持久化切换

account/order 已切换；risk/halt/snapshot、follow、audit、job execution 等路径仍未全部切换为数据库事实。

## 7. 事务注意事项

`PaperPersistenceTransactionService` 当前使用显式 MyBatis SqlSession commit/rollback。虽然 DataSourceTransactionManager Bean 已可装配，但最终 Spring `@Transactional` Proxy、生效调用路径和 dynamic-datasource 事务选择尚未完成专项验收。

## 8. 禁止范围

继续禁止：

- 前端实现；
- 根 POM、正式 YAML、Flyway/SQL 未获批修改；
- Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE；
- 生产数据库；
- Git 命令；
- 输出密码、Token、API Key、私钥或券商凭据；
- 数据库迁移和 DDL。

## 9. 下一步目标

```text
PHASE1B-RUNTIME-TRANSACTION-PROXY-AND-RISK-PERSISTENCE-CLOSURE
```

下一步：

1. 建立不触发外部基础设施的受控 WebApplicationContext/HTTP 隔离启动；
2. 明确 Spring `@Transactional` 或显式 SqlSession 的正式选择；
3. 完成 TradingHalt/RiskSnapshot persistence API；
4. 将风险状态 API 从内存服务切换到数据库事实；
5. 增加 partial fill、cancel release、sell frozen position 测试；
6. 关闭 P0 后再评估 Phase 1C。

## 10. Phase 1C 门禁

当前不允许进入 Phase 1C。

## 11. 模型建议

- 主控：GPT-5.6 Sol；
- 批量实现：deepseek-v4-pro，仅限明确白名单；
- 独立审阅：GLM-5.3，重点审阅 Spring transaction proxy、dynamic-datasource、风险持久化和 Phase 1C 门禁。

本轮未执行任何 Git 命令。
