# Phase 1B MySQL Test Dependency Proposal

- 日期：2026-10-06
- 阶段：`PHASE1B-PERSISTENCE-DEPENDENCY-APPROVAL-AND-CONCURRENCY-TEST`
- 状态：`PROPOSAL-ONLY`
- 本轮未修改任何 POM。

## 1. 发现的问题

`TradingAccountMapperIntegrationTest` 已创建，但在 `quantpf-domain` Maven test classpath 中找不到：

```text
com.mysql.cj.jdbc.Driver
```

测试因此按安全规则自动 skip。当前离线 compile、Mapper XML 资源编译和本地 schema 检查均已完成，但 MyBatis SqlSession 端到端测试尚未执行。

## 2. 建议依赖

建议在 `quantpf-domain/pom.xml` 增加仅测试范围的 MySQL Connector 依赖：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>test</scope>
</dependency>
```

版本继续使用根 POM/BOM 管理，不在模块中硬编码版本。

## 3. 选择 test scope 的原因

- 只为本地 MyBatis integration test 提供 JDBC Driver；
- 不改变生产运行时依赖边界；
- 不自动连接数据库；
- 不影响 Redis、Broker、SnailJob、XXL-JOB、QMT、CTP、LIVE；
- 便于后续移除或替换为独立 integration-test profile。

## 4. 供应链和运行风险

- 依赖来自现有 RuoYi Maven 依赖体系；
- 版本由根 BOM 管理；
- test scope 不进入生产打包运行路径；
- 仍需人工批准后才能修改 POM；
- 本建议不包含任何密码或连接信息。

## 5. 预期验收

人工批准后执行：

```powershell
mvn -f "D:\workspace\quant\RuoYi-Vue-Plus\pom.xml" `
  -pl quantpf-modules/quantpf-domain `
  -am `
  '-Dtest=TradingAccountMapperIntegrationTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' `
  test
```

当前测试只允许连接本地：

```text
127.0.0.1:3306/quantpf
```

测试必须使用事务 rollback，不得保留测试数据。

## 6. 回滚

仅回滚 `quantpf-domain/pom.xml` 中新增的 test scope dependency；不执行 Git reset、clean 或其他破坏性命令。

## 7. 当前审批结论

```text
POM_MODIFICATION_REQUIRED: YES
HUMAN_APPROVAL_REQUIRED: YES
CURRENT_ACTION: STOPPED_BEFORE_POM_EDIT
```