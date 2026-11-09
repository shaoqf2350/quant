package com.quantpf.job.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-JOB 执行器配置（ADR-10）。
 * <p>默认关闭（xxl.job.enabled=false），避免本地无调度中心/端口占用时影响应用启动；部署 xxl-job-admin 后置为 true。</p>
 * <p>执行器 appname=quant-executor、端口 9999，与《详设_9_部署详细设计方案》Docker Compose 网络一致。</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "xxl.job", name = "enabled", havingValue = "true")
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port:9999}")
    private int port;

    @Value("${xxl.job.executor.logpath:./logs/xxl-job}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays:30}")
    private int logRetentionDays;

    /**
     * XXL-JOB 执行器 Bean（Spring 容器中 @XxlJob 注解方法会自动注册为 JobHandler）。
     */
    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor() {
        log.info("初始化 XXL-JOB 执行器，appname={}，port={}，adminAddresses={}", appname, port, adminAddresses);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setAccessToken(accessToken);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}