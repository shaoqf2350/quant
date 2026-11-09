package com.quantpf.web.controller;

import com.quantpf.domain.repository.AuditRecordRepository;
import com.quantpf.domain.repository.JobExecutionAuditRepository;
import com.quantpf.finance.PersistentAuditApplicationService;
import com.quantpf.finance.PersistentJobExecutionApplicationService;
import com.quantpf.web.config.QuantPfPaperRuntimeConfiguration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(QuantPfRemainingApiPersistenceWebIntegrationTest.TestConfig.class)
public class QuantPfRemainingApiPersistenceWebIntegrationTest {
    @Autowired
    private ApplicationContext applicationContext;

    private MockMvc mockMvc;
    private String url;
    private String user;
    private String password;
    private String prefix;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        user = System.getenv("QPF_DB_USER");
        password = System.getenv("QPF_DB_PASSWORD");
        url = System.getenv().getOrDefault("QPF_JDBC_URL", "jdbc:mysql://127.0.0.1:3306/quantpf?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
        org.junit.jupiter.api.Assumptions.assumeTrue(user != null && password != null, "local DB credentials not supplied");
        prefix = "RAP_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mockMvc = MockMvcBuilders.standaloneSetup(
            applicationContext.getBean(QuantPaperAuditController.class),
            applicationContext.getBean(QuantPaperJobExecutionController.class)).build();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = connection()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO quantpf_audit_record (request_id, actor_id, action, aggregate_type, aggregate_id, result, detail) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, prefix + "_AUDIT_REQ");
                ps.setString(2, prefix + "_ACTOR");
                ps.setString(3, prefix + "_ACTION");
                ps.setString(4, "PAPER");
                ps.setString(5, prefix + "_AGG");
                ps.setString(6, "SUCCESS");
                ps.setString(7, "{\"source\":\"remaining-api-test\"}");
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO quantpf_job_execution_audit (job_name, execution_id, scheduler_type, status, request_id) VALUES (?, ?, 'PAPER', 'SUCCESS', ?)")) {
                ps.setString(1, prefix + "_JOB");
                ps.setString(2, prefix + "_EXEC");
                ps.setString(3, prefix + "_JOB_REQ");
                ps.executeUpdate();
            }
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (prefix == null) return;
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quantpf_audit_record WHERE request_id LIKE ?")) {
                ps.setString(1, prefix + "%");
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quantpf_job_execution_audit WHERE request_id LIKE ?")) {
                ps.setString(1, prefix + "%");
                ps.executeUpdate();
            }
            connection.commit();
        }
    }

    @org.junit.jupiter.api.Test
    void auditApiReadsPersistentAuditRowsWithRequestFilter() throws Exception {
        assertNotNull(applicationContext.getBean(SqlSessionFactory.class));
        mockMvc.perform(get("/api/quant/audit/page")
                .param("pageNum", "1")
                .param("pageSize", "20")
                .param("request_id", prefix + "_AUDIT_REQ"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].action").value(prefix + "_ACTION"))
            .andExpect(jsonPath("$.data[0].requestId").value(prefix + "_AUDIT_REQ"))
            .andExpect(jsonPath("$.data[0].actorId").value(prefix + "_ACTOR"))
            .andExpect(jsonPath("$.data[0].aggregateType").value("PAPER"))
            .andExpect(jsonPath("$.data[0].result").value("SUCCESS"));
    }

    @org.junit.jupiter.api.Test
    void jobExecutionApiReadsPersistentJobRowsWithFilters() throws Exception {
        mockMvc.perform(get("/api/quant/job/execution/page")
                .param("pageNum", "1")
                .param("pageSize", "20")
                .param("jobName", prefix + "_JOB")
                .param("status", "SUCCESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].jobName").value(prefix + "_JOB"))
            .andExpect(jsonPath("$.data[0].executionId").value(prefix + "_EXEC"))
            .andExpect(jsonPath("$.data[0].status").value("SUCCESS"));
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(url, user, password);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({QuantPfPaperRuntimeConfiguration.class, QuantPaperAuditController.class, QuantPaperJobExecutionController.class})
    static class TestConfig {
        @Bean
        DataSource dataSource() {
            String testUser = System.getenv("QPF_DB_USER");
            String testPassword = System.getenv("QPF_DB_PASSWORD");
            String testUrl = System.getenv().getOrDefault("QPF_JDBC_URL", "jdbc:mysql://127.0.0.1:3306/quantpf?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
            return new DriverManagerDataSource(testUrl, testUser, testPassword);
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
            factoryBean.setDataSource(dataSource);
            factoryBean.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver().getResources("classpath*:mapper/quantpf/*.xml"));
            return factoryBean.getObject();
        }
    }
}

class QuantPfRemainingApiNoFallbackUnitTest {
    @org.junit.jupiter.api.Test
    void auditServicePropagatesDatabaseFailureInsteadOfEmptyList() {
        AuditRecordRepository failing = new AuditRecordRepository() {
            @Override public java.util.List<com.quantpf.domain.persistence.record.AuditRecordRow> page(long offset, int limit, String requestId, String aggregateType, String aggregateId) { throw new RuntimeException("simulated db failure"); }
            @Override public int append(com.quantpf.domain.persistence.record.AuditRecordRow row) { throw new UnsupportedOperationException(); }
        };
        PersistentAuditApplicationService service = new PersistentAuditApplicationService(failing);
        assertThrows(RuntimeException.class, () -> service.page(1, 20));
    }

    @org.junit.jupiter.api.Test
    void jobExecutionServicePropagatesDatabaseFailureInsteadOfEmptyList() {
        JobExecutionAuditRepository failing = (offset, limit, jobName, executionId, status) -> {
            throw new RuntimeException("simulated db failure");
        };
        PersistentJobExecutionApplicationService service = new PersistentJobExecutionApplicationService(failing);
        assertThrows(RuntimeException.class, () -> service.page(1, 20));
    }
}
