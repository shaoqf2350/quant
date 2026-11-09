package com.quantpf.web.controller;

import com.quantpf.finance.PersistentPaperFollowApplicationService;
import com.quantpf.finance.PaperFollowPersistenceTransactionService;
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
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(QuantPaperFollowPersistenceIntegrationTest.TestConfig.class)
public class QuantPaperFollowPersistenceIntegrationTest {
    @Autowired private ApplicationContext applicationContext;
    @Autowired private DataSource dataSource;
    @Autowired private PersistentPaperFollowApplicationService service;
    @Autowired private SqlSessionFactory sqlSessionFactory;

    private String url;
    private String user;
    private String password;
    private String prefix;
    private Long accountId;
    private Long strategyId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        user = System.getenv("QPF_DB_USER");
        password = System.getenv("QPF_DB_PASSWORD");
        url = System.getenv().getOrDefault("QPF_JDBC_URL", "jdbc:mysql://127.0.0.1:3306/quantpf?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
        org.junit.jupiter.api.Assumptions.assumeTrue(user != null && password != null, "local DB credentials not supplied");
        prefix = "FOL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection c = connection()) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO quantpf_trading_account (account_code, account_name, account_type, currency, initial_cash, available_cash, frozen_cash, status) VALUES (?, ?, 'PAPER', 'CNY', 100000, 100000, 0, 'ACTIVE')", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, prefix + "_ACCOUNT"); ps.setString(2, prefix + " Account"); ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); accountId = rs.getLong(1); }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO quantpf_strategy (strategy_code, strategy_name, version, status, definition) VALUES (?, ?, 'v1', 'ACTIVE', '{\"kind\":\"paper-follow-test\"}')", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, prefix + "_STRATEGY"); ps.setString(2, prefix + " Strategy"); ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); strategyId = rs.getLong(1); }
            }
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (prefix == null) return;
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM quantpf_paper_follow_session WHERE start_request_id LIKE ? OR stop_request_id LIKE ?")) { ps.setString(1, prefix + "%"); ps.setString(2, prefix + "%"); ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM quantpf_audit_record WHERE request_id LIKE ?")) { ps.setString(1, prefix + "%"); ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM quantpf_strategy WHERE id=?")) { ps.setLong(1, strategyId); ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM quantpf_trading_account WHERE id=?")) { ps.setLong(1, accountId); ps.executeUpdate(); }
            c.commit();
        }
    }

    @org.junit.jupiter.api.Test
    void startStopReplayAndDatabasePersistence() throws Exception {
        var started = service.start(accountId, strategyId, "PAPER", prefix + "_START");
        assertEquals("RUNNING", started.status());
        var replay = service.start(accountId, strategyId, "PAPER", prefix + "_START");
        assertTrue(replay.idempotent());
        var stopped = service.stop(started.sessionId(), prefix + "_STOP", "test stop");
        assertEquals("STOPPED", stopped.status());
        var stopReplay = service.stop(started.sessionId(), prefix + "_STOP", "different reason");
        assertTrue(stopReplay.idempotent());
        assertEquals(1L, count("SELECT COUNT(*) FROM quantpf_paper_follow_session WHERE session_id=? AND status='STOPPED'", started.sessionId()));
        assertEquals(2L, count("SELECT COUNT(*) FROM quantpf_audit_record WHERE request_id IN (?,?)", prefix + "_START", prefix + "_STOP"));
        var newSession = service.start(accountId, strategyId, "PAPER", prefix + "_START_2");
        assertFalse(newSession.sessionId().equals(started.sessionId()));
        assertEquals(1L, count("SELECT COUNT(*) FROM quantpf_paper_follow_session WHERE account_id=? AND strategy_id=? AND status='RUNNING'", accountId, strategyId));
    }

    @org.junit.jupiter.api.Test
    void sameDimensionConflictAndConcurrentStartHaveOneRunningRow() throws Exception {
        var first = service.start(accountId, strategyId, "PAPER", prefix + "_FIRST");
        assertThrows(RuntimeException.class, () -> service.start(accountId, strategyId, "PAPER", prefix + "_SECOND"));
        service.stop(first.sessionId(), prefix + "_STOP", "release");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<String>> calls = List.of(
                () -> service.start(accountId, strategyId, "PAPER", prefix + "_C1").sessionId(),
                () -> service.start(accountId, strategyId, "PAPER", prefix + "_C2").sessionId());
            List<Future<String>> results = executor.invokeAll(calls);
            int successes = 0;
            int failures = 0;
            for (Future<String> result : results) { try { assertNotNull(result.get()); successes++; } catch (Exception expected) { failures++; } }
            assertEquals(1, successes);
            assertEquals(1, failures);
            assertEquals(1L, count("SELECT COUNT(*) FROM quantpf_paper_follow_session WHERE account_id=? AND strategy_id=? AND status='RUNNING'", accountId, strategyId));
        } finally { executor.shutdownNow(); }
    }

    @org.junit.jupiter.api.Test
    void startDoesNotWaitBehindUncommittedStopLock() throws Exception {
        var started = service.start(accountId, strategyId, "PAPER", prefix + "_LOCK_START");
        try (Connection locker = connection()) {
            locker.setAutoCommit(false);
            try (PreparedStatement ps = locker.prepareStatement("SELECT id FROM quantpf_paper_follow_session WHERE session_id=? FOR UPDATE")) {
                ps.setString(1, started.sessionId());
                try (ResultSet rs = ps.executeQuery()) { assertTrue(rs.next()); }
            }
            RuntimeException conflict = assertThrows(RuntimeException.class,
                () -> service.start(accountId, strategyId, "PAPER", prefix + "_LOCKED_RETRY"));
            assertTrue(conflict.getMessage() == null || conflict.getMessage().contains("QPF-FOLLOW-001") || conflict.getCause() != null);
            locker.rollback();
        }
        service.stop(started.sessionId(), prefix + "_LOCK_STOP", "release lock");
    }
    @org.junit.jupiter.api.Test
    void auditFailureRollsBackFollowSession() throws Exception {
        PaperFollowPersistenceTransactionService failing = new PaperFollowPersistenceTransactionService(
            sqlSessionFactory, () -> { throw new IllegalStateException("simulated audit failure"); });
        assertThrows(RuntimeException.class, () -> failing.start(accountId, strategyId, "PAPER", prefix + "_ROLLBACK"));
        assertEquals(0L, count("SELECT COUNT(*) FROM quantpf_paper_follow_session WHERE start_request_id=?", prefix + "_ROLLBACK"));
        assertEquals(0L, count("SELECT COUNT(*) FROM quantpf_audit_record WHERE request_id=?", prefix + "_ROLLBACK"));
    }
    @org.junit.jupiter.api.Test
    void controllerRejectsMissingIdempotencyKey() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(applicationContext.getBean(QuantPaperFollowController.class)).build();
        mvc.perform(post("/api/quant/paper/follow-start").contentType("application/json").content("{\"accountId\":1,\"strategyId\":1,\"mode\":\"PAPER\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    private Connection connection() throws Exception { return DriverManager.getConnection(url, user, password); }
    private long count(String sql, Object... args) throws Exception { try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) { for (int i=0;i<args.length;i++) ps.setObject(i+1,args[i]); try(ResultSet rs=ps.executeQuery()){rs.next();return rs.getLong(1);} } }

    @Configuration(proxyBeanMethods = false)
    @Import({QuantPfPaperRuntimeConfiguration.class, QuantPaperFollowController.class})
    static class TestConfig {
        @Bean DataSource dataSource() { return new DriverManagerDataSource(System.getenv().getOrDefault("QPF_JDBC_URL", "jdbc:mysql://127.0.0.1:3306/quantpf"), System.getenv("QPF_DB_USER"), System.getenv("QPF_DB_PASSWORD")); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception { SqlSessionFactoryBean b=new SqlSessionFactoryBean(); b.setDataSource(dataSource); b.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver().getResources("classpath*:mapper/quantpf/*.xml")); return b.getObject(); }
    }
}


