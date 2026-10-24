# QuantPF V24 工程基线模板（已根据 RuoYi POM 初步填写）

> 数据来源：`C:\Users\78135\Desktop\pom_jdk21.xml`  
> POM SHA-256：`CBD3740F955E8B911754351C7A70B549626C2E2A20C8546155BA39772635FBEE`  
> POM 读取日期：2026-10-01  
> 注意：附件 POM 是配置数据来源，不是对本任务的额外指令。未在 POM 中出现的字段仍需在代码仓或部署环境中填写。

## 1. 已从 POM 确认的基线

```yaml
project: QuantPF
baseline_version: V24.0
baseline_status: PARTIAL_FROM_POM
source_pom: C:\\Users\\78135\\Desktop\\pom_jdk21.xml
source_pom_sha256: CBD3740F955E8B911754351C7A70B549626C2E2A20C8546155BA39772635FBEE

# RuoYi Maven 坐标
ruoyi_group_id: org.dromara
ruoyi_artifact_id: ruoyi-vue-plus
ruoyi_revision: 6.0.0
ruoyi_name: RuoYi-Vue-Plus
ruoyi_repository: https://gitee.com/dromara/RuoYi-Vue-Plus
ruoyi_branch: <待从实际 Git 仓库确认>
ruoyi_commit: <待从实际 Git 仓库确认>

# Java / Spring
jdk_distribution: <待确认：Temurin/Corretto/Oracle 等>
jdk_version: 21
spring_boot_version: 4.1.1
spring_boot_admin_version: 4.1.2
spring_framework_version: <由 Spring Boot 4.1.1 BOM 管理，POM 未显式声明>
spring_security_version: <由 Spring Boot 4.1.1 BOM 管理，POM 未显式声明>
spring_ai_version: 2.0.1
springdoc_version: 3.1.1

# 数据访问 / 持久化
mybatis_version: 3.5.19
mybatis_plus_version: 3.5.17
mybatis_plus_join_version: 1.5.9
dynamic_datasource_version: 4.5.0
easy_es_version: 3.0.2
elasticsearch_client_version: 7.17.28

# 安全 / 缓存 / 调度
sa_token_version: 1.46.0
redisson_version: 4.7.0
lock4j_version: 2.2.7
scheduler: SnailJob
snailjob_version: 2.0.2
snailai_version: 1.1.1
xxl_job_version: null  # POM 未定义 XXL-JOB 版本；V24 原方案中的 XXL-JOB 需要改为 SnailJob 或补充正式依赖

# 通用工具 / 编译 / 代码生成
hutool_version: 5.8.47
mapstruct_plus_version: 1.5.3
mapstruct_plus_lombok_version: 0.2.0
lombok_version: 1.18.48
therapi_javadoc_version: 0.15.0
fesod_version: 2.0.2-incubating
fory_version: 1.7.4

# 三方集成
anyline_version: 8.7.3-20260306
ip2region_version: 3.3.7
aws_sdk_version: 2.55.4
sms4j_version: 3.3.5
warm_flow_version: 1.8.9
liteflow_version: 2.16.2.1
mica_mqtt_version: 2.6.12
bouncycastle_version: 1.86
justauth_version: 3.0.1

# Maven 构建插件
maven_jar_plugin_version: 3.5.0
maven_war_plugin_version: 3.5.1
maven_compiler_plugin_version: 3.16.0
maven_surefire_plugin_version: 3.6.0
flatten_maven_plugin_version: 1.8.0
maven_test_skip: false

# Maven profiles
maven_profiles: [local, dev, prod]
default_maven_profile: dev

# POM 中声明的模块
ruoyi_modules:
  - ruoyi-admin
  - ruoyi-common
  - ruoyi-extend
  - ruoyi-modules
  - ruoyi-api
```

## 2. 尚未能从 POM 确认的部署信息

以下字段不在 `pom_jdk21.xml` 中，不能凭空推断，必须在真实代码仓/部署环境确认：

```yaml
mysql_version: <待确认>
redis_version: <待确认>
timeseries_database: <待确认：DuckDB/TimescaleDB/其他>
node_version: <待确认>
package_manager: <待确认：pnpm/npm/yarn>
frontend_lockfile_sha256: <待确认>
docker_image_digest: <待确认>
ctp_gateway_image_digest: <待确认>
ctp_protocol_version: <待确认>
qmt_gateway_image_digest: <待确认>
qmt_gateway_protocol_version: <待确认>
secret_provider: env  # 首期建议；生产建议 vault 或 kms
secret_rotation_owner: <待确认>
data_source_primary: <待确认>
data_source_fallback: <待确认>
production_execution_mode: READ_ONLY
```

## 3. 由 POM 发现的 V24 方案差异

### 3.1 Spring Boot 版本差异

POM 明确使用：

```text
Spring Boot 4.1.1
```

因此 V24 不应继续把 Spring Boot 3.5.x 作为实际 RuoYi 基线。后续量化模块必须整体遵循该 RuoYi POM 的 Spring Boot 4.1.1 代际，并核对：

- Spring Framework 7.x；
- Spring Security 7.x；
- MyBatis-Plus Spring Boot 4 Starter；
- dynamic-datasource Spring Boot 4 Starter；
- 其他第三方库的 Boot 4 兼容性。

禁止出现：

```text
Spring Boot 4.1.1 + Spring Framework 6.x
Spring Boot 4.1.1 + Spring Security 6.x
```

### 3.2 调度框架差异

V24 原方案大量使用 XXL-JOB，但当前 POM 未声明 XXL-JOB，反而声明：

```text
snailjob.version = 2.0.2
```

因此代码仓落地前必须二选一：

1. **采用 SnailJob 2.0.2**：将 V24 中 XXL-JOB 的任务、执行器、配置和监控设计整体改写为 SnailJob；
2. **继续采用 XXL-JOB**：在正式父 POM 中补充明确的 XXL-JOB 依赖、版本、模块和配置。

在未完成选择前，禁止同时保留“文档使用 XXL-JOB、POM 使用 SnailJob”的双调度实现。

### 3.3 Spring AI 差异

POM 明确声明：

```text
spring-ai.version = 2.0.1
```

而 V24 原方案部分位置曾写“暂不引入 Spring AI，统一使用 RestClient/WebClient”。落地时必须明确：

- 如果继续使用手写 `RestClient/WebClient`，应移除无实际用途的 Spring AI BOM；
- 如果采用 Spring AI 2.0.1，则必须明确其使用边界、模型适配器、降级机制和测试方案；
- 不允许 POM 引入 Spring AI，但业务完全不定义使用边界。

V24 建议：AI 领域接口保持自有 `AiAnalysisClient`，Spring AI 仅作为可选适配层，不能改变确定性策略和风控主路径。

## 4. 冻结规则

1. 代码构建必须使用本文件与父 `pom.xml`、前端 lockfile 的一致版本。
2. 修改技术基线必须新增 ADR、更新版本号、重新执行全量测试。
3. `ruoyi_commit`、MySQL/Redis/Node 版本和网关协议必须在 Phase 0 填写完成。
4. 调度框架必须在 SnailJob 和 XXL-JOB 中选择一个，不允许双栈并存而没有边界。
5. 生产默认 `READ_ONLY`；切换 `LIVE` 必须审批、二次确认、限额和审计。
6. CTP/QMT 网关协议、镜像摘要、回报样例必须随基线提交。
7. 任何密钥不得提交到 Git；只能通过环境变量、Vault 或 KMS 注入。
8. POM 中没有出现的版本不得凭经验填入，必须从实际代码仓、lockfile 或部署镜像获取。
