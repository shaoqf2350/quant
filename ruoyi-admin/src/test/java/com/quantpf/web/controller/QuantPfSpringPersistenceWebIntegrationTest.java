package com.quantpf.web.controller;

import com.quantpf.web.config.QuantPfPaperRuntimeConfiguration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.GenericWebApplicationContext;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(QuantPfSpringPersistenceWebIntegrationTest.TestConfig.class)
class QuantPfSpringPersistenceWebIntegrationTest {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    private MockMvc mockMvc;
    private String url;
    private String user;
    private String password;
    private String prefix;
    private long accountId;
    private long instrumentId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        user = System.getenv("QPF_DB_USER");
        password = System.getenv("QPF_DB_PASSWORD");
        url = System.getenv().getOrDefault("QPF_JDBC_URL", "jdbc:mysql://127.0.0.1:3306/quantpf?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
        org.junit.jupiter.api.Assumptions.assumeTrue(user != null && password != null, "local DB credentials not supplied");
        prefix = "SPRING_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mockMvc = MockMvcBuilders.standaloneSetup(applicationContext.getBean(QuantPfHealthController.class), applicationContext.getBean(QuantPaperAccountController.class), applicationContext.getBean(QuantPaperOrderController.class), applicationContext.getBean(QuantPaperRiskController.class)).build();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = connection()) {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO quantpf_instrument (instrument_code, product_type, exchange_code, instrument_name, currency, lot_size, status) VALUES (?, 'STOCK', 'SSE', ?, 'CNY', 1, 'ACTIVE')", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, prefix + "_I");
                ps.setString(2, prefix + " instrument");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { assertNotNull(keys); keys.next(); instrumentId = keys.getLong(1); }
            }
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO quantpf_trading_account (account_code, account_name, account_type, currency, initial_cash, available_cash, frozen_cash, status) VALUES (?, ?, 'PAPER', 'CNY', 10000.00000000, 10000.00000000, 0.00000000, 'ACTIVE')", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, prefix + "_A");
                ps.setString(2, prefix + " account");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { assertNotNull(keys); keys.next(); accountId = keys.getLong(1); }
            }
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (accountId == 0) return;
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quantpf_cash_ledger WHERE account_id=?")) { ps.setLong(1, accountId); ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quantpf_risk_snapshot WHERE account_id IS NULL AND reason LIKE ?")) { ps.setString(1, prefix + "%"); ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quantpf_trading_halt WHERE scope='GLOBAL' AND reason LIKE ?")) { ps.setString(1, prefix + "%"); ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quantpf_paper_execution WHERE order_id IN (SELECT id FROM quantpf_paper_order WHERE account_id=?)")) { ps.setLong(1, accountId); ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quantpf_position WHERE account_id=?")) { ps.setLong(1, accountId); ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quantpf_paper_order WHERE account_id=?")) { ps.setLong(1, accountId); ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quantpf_trading_account WHERE id=?")) { ps.setLong(1, accountId); ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM quantpf_instrument WHERE id=?")) { ps.setLong(1, instrumentId); ps.executeUpdate(); }
            connection.commit();
        }
    }

    @org.junit.jupiter.api.Test
    void springContextAndHealthEndpointAreAvailable() throws Exception {
        assertNotNull(applicationContext.getBean(SqlSessionFactory.class));
        assertNotNull(applicationContext.getBean(QuantPfPaperRuntimeConfiguration.class));
        mockMvc.perform(get("/api/quant/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.liveEnabled").value(false));
    }

    @org.junit.jupiter.api.Test
    void riskApiReadsAndWritesPersistentHaltAndSnapshotFacts() throws Exception {
        String haltReason = prefix + "_spring-persistent-halt";
        String resumeReason = prefix + "_spring-persistent-resume";
        mockMvc.perform(post("/api/quant/risk/halt")
                .contentType("application/json")
                .content("{\"reason\":\"" + haltReason + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("HALTED"))
            .andExpect(jsonPath("$.data.reason").value(haltReason));

        mockMvc.perform(get("/api/quant/risk/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("HALTED"))
            .andExpect(jsonPath("$.data.reason").value(haltReason));

        assertEquals(1L, queryLong("SELECT COUNT(*) FROM quantpf_trading_halt WHERE scope='GLOBAL' AND active=1 AND reason=?", haltReason));
        assertEquals(1L, queryLong("SELECT COUNT(*) FROM quantpf_risk_snapshot WHERE account_id IS NULL AND risk_status='HALTED' AND reason=?", haltReason));

        String haltedOrder = "{\"accountId\":" + accountId + ",\"instrumentId\":" + instrumentId + ",\"side\":\"BUY\",\"orderType\":\"LIMIT\",\"quantity\":2,\"limitPrice\":10,\"estimatedNotional\":20,\"fillPrice\":10,\"clientOrderId\":\"" + prefix + "_HALTED_CLIENT\",\"requestId\":\"" + prefix + "_HALTED_REQUEST\"}";
        mockMvc.perform(post("/api/quant/order/paper").header("Idempotency-Key", prefix + "_HALTED_IDEMP").contentType("application/json").content(haltedOrder))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/api/quant/risk/resume")
                .contentType("application/json")
                .content("{\"reason\":\"" + resumeReason + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CLEAR"));

        assertEquals(0L, queryLong("SELECT COUNT(*) FROM quantpf_trading_halt WHERE scope='GLOBAL' AND active=1 AND reason=?", haltReason));
        assertEquals(1L, queryLong("SELECT COUNT(*) FROM quantpf_risk_snapshot WHERE account_id IS NULL AND risk_status='CLEAR' AND reason=?", resumeReason));
    }

    @org.junit.jupiter.api.Test
    void accountAndOrderApisReadAndWriteDatabaseFacts() throws Exception {
        mockMvc.perform(get("/api/quant/account/page").param("pageNum", "1").param("pageSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].accountCode").value(prefix + "_A"));

        String request = "{\"accountId\":" + accountId + ",\"instrumentId\":" + instrumentId + ",\"side\":\"BUY\",\"orderType\":\"LIMIT\",\"quantity\":2,\"limitPrice\":10,\"estimatedNotional\":20,\"fillPrice\":10,\"clientOrderId\":\"" + prefix + "_CLIENT\",\"requestId\":\"" + prefix + "_REQUEST\"}";
        mockMvc.perform(post("/api/quant/order/paper").header("Idempotency-Key", prefix + "_IDEMP").contentType("application/json").content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("FILLED"));

        assertEquals(1L, queryLong("SELECT COUNT(*) FROM quantpf_paper_order WHERE account_id=? AND client_order_id=?", accountId, prefix + "_CLIENT"));
        assertEquals(new BigDecimal("9980.00000000").setScale(8), queryDecimal("SELECT available_cash FROM quantpf_trading_account WHERE id=?", accountId));
    }

    private Connection connection() throws Exception { return DriverManager.getConnection(url, user, password); }
    private long queryLong(String sql, Object... args) throws Exception { try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); } } }
    private BigDecimal queryDecimal(String sql, Object... args) throws Exception { try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1).setScale(8); } } }
    private static void bind(PreparedStatement ps, Object[] args) throws Exception { for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]); }

    @Configuration(proxyBeanMethods = false)
    @Import({QuantPfPaperRuntimeConfiguration.class, QuantPfHealthController.class, QuantPaperAccountController.class, QuantPaperOrderController.class, QuantPaperRiskController.class})
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





