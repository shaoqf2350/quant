package com.quantpf.web.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QuantPF 雪花主键生成器配置。
 * <p>workerId / datacenterId 通过环境变量或 YAML 覆盖（多实例部署时保证唯一）。</p>
 */
@Configuration(proxyBeanMethods = false)
public class QuantPfSnowflakeConfiguration {

    /**
     * 构造 Hutool Snowflake 生成器（业务表主键 pk_id 统一使用）。
     *
     * @param workerId     机器标识（0~31）
     * @param datacenterId 数据中心标识（0~31）
     * @return Snowflake 实例
     */
    @Bean
    public Snowflake quantpfSnowflake(
        @Value("${quantpf.snowflake.worker-id:1}") int workerId,
        @Value("${quantpf.snowflake.datacenter-id:1}") int datacenterId) {
        return IdUtil.getSnowflake(workerId, datacenterId);
    }
}