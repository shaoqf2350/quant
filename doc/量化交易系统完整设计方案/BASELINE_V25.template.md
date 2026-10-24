# QuantPF V25 工程基线模板（XXL-JOB 单一调度版）

> 数据来源：`C:\Users\78135\Desktop\pom_jdk21.xml` 与 V25 调度决策。  
> 原 POM SHA-256：`CBD3740F955E8B911754351C7A70B549626C2E2A20C8546155BA39772635FBEE`。  
> 当前状态：目标基线；落地代码仓后需重新计算修改后父 POM 的 SHA-256。

```yaml
project: QuantPF
baseline_version: V25.0
baseline_status: TARGET_XXL_JOB_MIGRATION
source_pom: C:\\Users\\78135\\Desktop\\pom_jdk21.xml
source_pom_sha256_before_migration: CBD3740F955E8B911754351C7A70B549626C2E2A20C8546155BA39772635FBEE
source_pom_sha256_after_migration: <落地后填写>

ruoyi_group_id: org.dromara
ruoyi_artifact_id: ruoyi-vue-plus
ruoyi_revision: 6.0.0
ruoyi_repository: https://gitee.com/dromara/RuoYi-Vue-Plus
ruoyi_branch: <从实际 Git 仓库确认>
ruoyi_commit: <从实际 Git 仓库确认>

jdk_distribution: <Temurin/Corretto/Oracle 等，待确认>
jdk_version: 21
spring_boot_version: 4.1.1
spring_boot_admin_version: 4.1.2
spring_framework_version: <由 Spring Boot 4.1.1 BOM 解析后填写>
spring_security_version: <由 Spring Boot 4.1.1 BOM 解析后填写>
spring_ai_version: 2.0.1
springdoc_version: 3.1.1

mybatis_version: 3.5.19
mybatis_plus_version: 3.5.17
mybatis_plus_join_version: 1.5.9
dynamic_datasource_version: 4.5.0
sa_token_version: 1.46.0
redisson_version: 4.7.0
lock4j_version: 2.2.7
hutool_version: 5.8.47
mapstruct_plus_version: 1.5.3
lombok_version: 1.18.48

# 唯一调度框架
scheduler: XXL-JOB
xxl_job_version: 3.1.0
xxl_job_admin_deployment: standalone
xxl_job_admin_database: xxl_job
xxl_job_executor_appname: quant-executor
xxl_job_executor_port: 9999
xxl_job_access_token_provider: env|vault|kms
snailjob_version: null
snailjob_client_allowed: false
snailjob_server_allowed: false
scheduler_dual_stack_allowed: false

# SnailAI 不是调度框架；仅在依赖树无 snail-job-client 时允许保留
snailai_version: 1.1.1
snailai_enabled: false
snailai_dependency_check: mvn dependency:tree -Dincludes=com.aizuda:snail-job-*

mysql_version: <待确认>
redis_version: <待确认>
timeseries_database: <DuckDB/TimescaleDB/其他，待确认>
node_version: <待确认>
package_manager: pnpm@10.34.5
frontend_lockfile_sha256: <待确认>
docker_image_digest: <待确认>
xxl_job_admin_image_digest: <待确认>
ctp_gateway_image_digest: <待确认>
ctp_protocol_version: <待确认>
qmt_gateway_image_digest: <待确认>
qmt_gateway_protocol_version: <待确认>
secret_provider: env
secret_rotation_owner: <待确认>
data_source_primary: <待确认>
data_source_fallback: <待确认>
production_execution_mode: READ_ONLY
```

## 调度迁移冻结规则

1. 父 POM 必须删除 `snailjob.version` 和 `snail-job-client-*`。
2. 父 POM 必须新增并冻结 `xxl-job.version=3.1.0`。
3. `ruoyi-common-job` 保留模块名，内部只允许 XXL-JOB 实现。
4. `ruoyi-snailjob-server` 不得参与 Maven Reactor、镜像构建和部署。
5. 生产运行时依赖树不得出现 SnailJob Client/Server。
6. SnailAI 若传递引入 SnailJob，必须排除依赖或移除 SnailAI。
7. XXL-JOB Admin 使用独立数据库和独立迁移，不写入 QuantPF Flyway 历史。
8. 所有 JobHandler 必须有业务幂等键和 `qo_job_execution` 记录。
9. 真实下单、移仓等资金风险任务默认重试次数为 0。
10. 生产默认 `READ_ONLY`，切换 `LIVE` 必须审批、限额、二次确认和审计。
