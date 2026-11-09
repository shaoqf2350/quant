package com.quantpf.finance;

import com.quantpf.domain.persistence.mapper.CashLedgerMapper;
import com.quantpf.domain.persistence.mapper.PaperExecutionMapper;
import com.quantpf.domain.persistence.mapper.PaperOrderMapper;
import com.quantpf.domain.persistence.mapper.PositionMapper;
import com.quantpf.domain.persistence.mapper.RiskSnapshotMapper;
import com.quantpf.domain.persistence.mapper.TradingAccountMapper;
import com.quantpf.domain.persistence.mapper.TradingHaltMapper;
import com.quantpf.domain.persistence.record.PaperOrderRow;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PaperPersistenceTransactionIntegrationTest {
    private String url;
    private String user;
    private String password;
    private long accountId;
    private long instrumentId;
    private String prefix;
    private SqlSessionFactory sessionFactory;
    private PaperPersistenceTransactionService service;

    @BeforeEach
    void setUp() throws Exception {
        user = System.getenv("QPF_DB_USER");
        password = System.getenv("QPF_DB_PASSWORD");
        url = System.getenv().getOrDefault("QPF_JDBC_URL", "jdbc:mysql://127.0.0.1:3306/quantpf?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
        Assumptions.assumeTrue(user != null && password != null, "local DB credentials not supplied");
        Class.forName("com.mysql.cj.jdbc.Driver");
        prefix = "P1B_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection c = connection()) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO quantpf_instrument (pk_id, instrument_code, product_type, exchange_code, instrument_name, currency, lot_size, status) VALUES (?, ?, 'STOCK', 'SSE', ?, 'CNY', 1, 'ACTIVE')", Statement.RETURN_GENERATED_KEYS)) {
                long testPkId = Math.abs(java.util.UUID.randomUUID().getLeastSignificantBits());
                ps.setLong(1, testPkId); ps.setString(2, prefix + "_I"); ps.setString(3, prefix + " instrument"); ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); instrumentId = keys.getLong(1); }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO quantpf_trading_account (account_code, account_name, account_type, currency, initial_cash, available_cash, frozen_cash, status) VALUES (?, ?, 'PAPER', 'CNY', 10000.00000000, 10000.00000000, 0.00000000, 'ACTIVE')", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, prefix + "_A"); ps.setString(2, prefix + " account"); ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); accountId = keys.getLong(1); }
            }
        }
        sessionFactory = createSessionFactory();
        service = new PaperPersistenceTransactionService(sessionFactory);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (accountId == 0) return;
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement("DELETE FROM quantpf_cash_ledger WHERE account_id=?")) { p1.setLong(1, accountId); p1.executeUpdate(); }
            try (PreparedStatement p2 = c.prepareStatement("DELETE FROM quantpf_paper_execution WHERE order_id IN (SELECT id FROM quantpf_paper_order WHERE account_id=?)")) { p2.setLong(1, accountId); p2.executeUpdate(); }
            try (PreparedStatement p3 = c.prepareStatement("DELETE FROM quantpf_position WHERE account_id=?")) { p3.setLong(1, accountId); p3.executeUpdate(); }
            try (PreparedStatement p4 = c.prepareStatement("DELETE FROM quantpf_paper_order WHERE account_id=?")) { p4.setLong(1, accountId); p4.executeUpdate(); }
            try (PreparedStatement p5 = c.prepareStatement("DELETE FROM quantpf_trading_account WHERE id=?")) { p5.setLong(1, accountId); p5.executeUpdate(); }
            try (PreparedStatement p6 = c.prepareStatement("DELETE FROM quantpf_instrument WHERE id=?")) { p6.setLong(1, instrumentId); p6.executeUpdate(); }
            c.commit();
        }
    }

    @Test
    void sameIdempotencyKeyCreatesOneOrderAndFreezesCashOnce() throws Exception {
        PaperOrderRow template = order("IDEMP", "CLIENT", new BigDecimal("100.00000000"), BigDecimal.ZERO);
        List<PaperPersistenceTransactionService.SubmissionResult> results = runConcurrently(
            () -> service.submit(copy(template)), () -> service.submit(copy(template)));
        assertEquals(2, results.size());
        assertEquals(1, results.stream().filter(r -> !r.duplicate()).count());
        assertEquals(1, results.stream().filter(PaperPersistenceTransactionService.SubmissionResult::duplicate).count());
        assertEquals(1L, queryLong("SELECT COUNT(*) FROM quantpf_paper_order WHERE account_id=?", accountId));
        assertEquals(new BigDecimal("9900.00000000"), queryDecimal("SELECT available_cash FROM quantpf_trading_account WHERE id=?", accountId));
        assertEquals(new BigDecimal("100.00000000"), queryDecimal("SELECT frozen_cash FROM quantpf_trading_account WHERE id=?", accountId));
    }

    @Test
    void sameExecutionIdSettlesOnceAndDoesNotDoubleMutateAccountOrPosition() throws Exception {
        PaperOrderRow template = order("SETTLE", "CLIENT-SETTLE", new BigDecimal("100.00000000"), BigDecimal.ZERO);
        PaperPersistenceTransactionService.SubmissionResult submitted = service.submit(template);
        PaperPersistenceTransactionService.SettlementCommand command = new PaperPersistenceTransactionService.SettlementCommand(submitted.orderId(), prefix + "_E", "EXEC-IDEMP-" + prefix, new BigDecimal("10.00000000"), new BigDecimal("10.00000000"), BigDecimal.ZERO, Instant.now(), null, false);
        List<PaperPersistenceTransactionService.SettlementResult> results = runConcurrently(() -> service.settle(command), () -> service.settle(command));
        assertEquals(2, results.size());
        assertEquals(1, results.stream().filter(r -> !r.duplicate()).count());
        assertEquals(1, results.stream().filter(PaperPersistenceTransactionService.SettlementResult::duplicate).count());
        assertEquals(1L, queryLong("SELECT COUNT(*) FROM quantpf_paper_execution WHERE order_id=?", submitted.orderId()));
        assertEquals(1L, queryLong("SELECT COUNT(*) FROM quantpf_cash_ledger WHERE account_id=?", accountId));
        assertEquals(new BigDecimal("9900.00000000"), queryDecimal("SELECT available_cash FROM quantpf_trading_account WHERE id=?", accountId));
        assertEquals(BigDecimal.ZERO.setScale(8), queryDecimal("SELECT frozen_cash FROM quantpf_trading_account WHERE id=?", accountId));
        assertEquals(new BigDecimal("10.00000000"), queryDecimal("SELECT quantity FROM quantpf_position WHERE account_id=? AND instrument_id=?", accountId, instrumentId));
        assertEquals("FILLED", queryString("SELECT status FROM quantpf_paper_order WHERE id=?", submitted.orderId()));
    }

    @Test
    void settlementFailureAfterLedgerRollsBackExecutionLedgerAccountAndPosition() throws Exception {
        PaperOrderRow template = order("ROLLBACK", "CLIENT-ROLLBACK", new BigDecimal("100.00000000"), BigDecimal.ZERO);
        PaperPersistenceTransactionService.SubmissionResult submitted = service.submit(template);
        PaperPersistenceTransactionService.SettlementCommand command = new PaperPersistenceTransactionService.SettlementCommand(submitted.orderId(), prefix + "_R", "ROLLBACK-IDEMP-" + prefix, new BigDecimal("10.00000000"), new BigDecimal("10.00000000"), BigDecimal.ZERO, Instant.now(), null, true);
        assertThrows(IllegalStateException.class, () -> service.settle(command));
        assertEquals(0L, queryLong("SELECT COUNT(*) FROM quantpf_paper_execution WHERE order_id=?", submitted.orderId()));
        assertEquals(0L, queryLong("SELECT COUNT(*) FROM quantpf_cash_ledger WHERE account_id=?", accountId));
        assertEquals(new BigDecimal("9900.00000000"), queryDecimal("SELECT available_cash FROM quantpf_trading_account WHERE id=?", accountId));
        assertEquals(new BigDecimal("100.00000000"), queryDecimal("SELECT frozen_cash FROM quantpf_trading_account WHERE id=?", accountId));
        assertEquals(0L, queryLong("SELECT COUNT(*) FROM quantpf_position WHERE account_id=? AND instrument_id=?", accountId, instrumentId));
        assertEquals("NEW", queryString("SELECT status FROM quantpf_paper_order WHERE id=?", submitted.orderId()));
    }

    @Test
    void sameClientOrderIdWithDifferentIdempotencyKeyCreatesOneOrder() throws Exception {
        PaperOrderRow first = order("CLIENT-DUP", "CLIENT-SAME", new BigDecimal("50.00000000"), BigDecimal.ZERO);
        PaperOrderRow second = copy(first);
        second.setIdempotencyKey(prefix + "_IDEMP_CLIENT_2");
        List<PaperPersistenceTransactionService.SubmissionResult> results = runConcurrently(
            () -> service.submit(copy(first)), () -> service.submit(copy(second)));
        assertEquals(1L, queryLong("SELECT COUNT(*) FROM quantpf_paper_order WHERE account_id=?", accountId));
        assertEquals(1, results.stream().filter(r -> !r.duplicate()).count());
        assertEquals(1, results.stream().filter(PaperPersistenceTransactionService.SubmissionResult::duplicate).count());
        assertEquals(new BigDecimal("9950.00000000"), queryDecimal("SELECT available_cash FROM quantpf_trading_account WHERE id=?", accountId));
    }

    @Test
    void sameLedgerEventIdWithDifferentExecutionIdDoesNotCreateSecondExecution() throws Exception {
        PaperOrderRow template = order("LEDGER", "CLIENT-LEDGER", new BigDecimal("100.00000000"), BigDecimal.ZERO);
        PaperPersistenceTransactionService.SubmissionResult submitted = service.submit(template);
        String ledgerKey = "LEDGER-IDEMP-" + prefix;
        PaperPersistenceTransactionService.SettlementCommand first = new PaperPersistenceTransactionService.SettlementCommand(submitted.orderId(), prefix + "_E1", prefix + "_EXEC1", new BigDecimal("10.00000000"), new BigDecimal("10.00000000"), BigDecimal.ZERO, Instant.now(), ledgerKey, false);
        PaperPersistenceTransactionService.SettlementCommand second = new PaperPersistenceTransactionService.SettlementCommand(submitted.orderId(), prefix + "_E2", prefix + "_EXEC2", new BigDecimal("10.00000000"), new BigDecimal("10.00000000"), BigDecimal.ZERO, Instant.now(), ledgerKey, false);
        PaperPersistenceTransactionService.SettlementResult firstResult = service.settle(first);
        PaperPersistenceTransactionService.SettlementResult secondResult = service.settle(second);
        assertFalse(firstResult.duplicate());
        assertTrue(secondResult.duplicate());
        assertEquals(1L, queryLong("SELECT COUNT(*) FROM quantpf_paper_execution WHERE order_id=?", submitted.orderId()));
        assertEquals(1L, queryLong("SELECT COUNT(*) FROM quantpf_cash_ledger WHERE account_id=?", accountId));
    }
    @Test
    void multipleExecutionsProducePartialThenFilledAndReleaseCashFreeze() throws Exception {
        PaperOrderRow partialOrder = order("PARTIAL", "CLIENT-PARTIAL", new BigDecimal("1000.00000000"), BigDecimal.ZERO); partialOrder.setQuantity(new BigDecimal("100.00000000")); PaperPersistenceTransactionService.SubmissionResult submitted = service.submit(partialOrder);
        service.settle(new PaperPersistenceTransactionService.SettlementCommand(submitted.orderId(), prefix + "_E1", prefix + "_EI1", new BigDecimal("30.00000000"), new BigDecimal("10.00000000"), BigDecimal.ZERO, Instant.now(), prefix + "_L1", false));
        assertEquals("PARTIALLY_FILLED", queryString("SELECT status FROM quantpf_paper_order WHERE id=?", submitted.orderId()));
        service.settle(new PaperPersistenceTransactionService.SettlementCommand(submitted.orderId(), prefix + "_E2", prefix + "_EI2", new BigDecimal("20.00000000"), new BigDecimal("10.00000000"), BigDecimal.ZERO, Instant.now(), prefix + "_L2", false));
        assertEquals("PARTIALLY_FILLED", queryString("SELECT status FROM quantpf_paper_order WHERE id=?", submitted.orderId()));
        service.settle(new PaperPersistenceTransactionService.SettlementCommand(submitted.orderId(), prefix + "_E3", prefix + "_EI3", new BigDecimal("50.00000000"), new BigDecimal("10.00000000"), BigDecimal.ZERO, Instant.now(), prefix + "_L3", false));
        assertEquals("FILLED", queryString("SELECT status FROM quantpf_paper_order WHERE id=?", submitted.orderId()));
        assertEquals(3L, queryLong("SELECT COUNT(*) FROM quantpf_paper_execution WHERE order_id=?", submitted.orderId()));
        assertEquals(BigDecimal.ZERO.setScale(8), queryDecimal("SELECT frozen_cash FROM quantpf_trading_account WHERE id=?", accountId));
        assertEquals(new BigDecimal("9000.00000000"), queryDecimal("SELECT available_cash FROM quantpf_trading_account WHERE id=?", accountId));
    }

    @Test
    void cancelReleasesRemainingCashFreezeExactlyOnce() throws Exception {
        PaperPersistenceTransactionService.SubmissionResult submitted = service.submit(order("CANCEL", "CLIENT-CANCEL", new BigDecimal("100.00000000"), BigDecimal.ZERO));
        PaperPersistenceTransactionService.CancelResult first = service.cancel(submitted.orderId(), prefix + " cancel");
        PaperPersistenceTransactionService.CancelResult second = service.cancel(submitted.orderId(), prefix + " duplicate cancel");
        assertFalse(first.duplicate());
        assertTrue(second.duplicate());
        assertEquals("CANCELED", queryString("SELECT status FROM quantpf_paper_order WHERE id=?", submitted.orderId()));
        assertEquals(new BigDecimal("10000.00000000"), queryDecimal("SELECT available_cash FROM quantpf_trading_account WHERE id=?", accountId));
        assertEquals(BigDecimal.ZERO.setScale(8), queryDecimal("SELECT frozen_cash FROM quantpf_trading_account WHERE id=?", accountId));
    }

    @Test
    void sellExecutionsReduceFrozenPositionAndIncreaseCash() throws Exception {
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement("INSERT INTO quantpf_position (account_id, instrument_id, quantity, available_quantity, frozen_quantity, average_cost, realized_pnl) VALUES (?, ?, 100, 100, 0, 10, 0)")) { ps.setLong(1, accountId); ps.setLong(2, instrumentId); ps.executeUpdate(); }
        PaperOrderRow sell = order("SELL", "CLIENT-SELL", BigDecimal.ZERO, new BigDecimal("100.00000000"));
        sell.setSide("SELL"); sell.setQuantity(new BigDecimal("100.00000000"));
        PaperPersistenceTransactionService.SubmissionResult submitted = service.submit(sell);
        service.settle(new PaperPersistenceTransactionService.SettlementCommand(submitted.orderId(), prefix + "_SE1", prefix + "_SEI1", new BigDecimal("40.00000000"), new BigDecimal("12.00000000"), BigDecimal.ZERO, Instant.now(), prefix + "_SL1", false));
        assertEquals("PARTIALLY_FILLED", queryString("SELECT status FROM quantpf_paper_order WHERE id=?", submitted.orderId()));
        service.settle(new PaperPersistenceTransactionService.SettlementCommand(submitted.orderId(), prefix + "_SE2", prefix + "_SEI2", new BigDecimal("60.00000000"), new BigDecimal("12.00000000"), BigDecimal.ZERO, Instant.now(), prefix + "_SL2", false));
        assertEquals("FILLED", queryString("SELECT status FROM quantpf_paper_order WHERE id=?", submitted.orderId()));
        assertEquals(BigDecimal.ZERO.setScale(8), queryDecimal("SELECT quantity FROM quantpf_position WHERE account_id=? AND instrument_id=?", accountId, instrumentId));
        assertEquals(BigDecimal.ZERO.setScale(8), queryDecimal("SELECT frozen_quantity FROM quantpf_position WHERE account_id=? AND instrument_id=?", accountId, instrumentId));
        assertEquals(new BigDecimal("11200.00000000"), queryDecimal("SELECT available_cash FROM quantpf_trading_account WHERE id=?", accountId));
    }

    private PaperOrderRow order(String suffix, String client, BigDecimal frozenCash, BigDecimal frozenQuantity) {
        PaperOrderRow row = new PaperOrderRow();
        row.setAccountId(accountId); row.setInstrumentId(instrumentId); row.setClientOrderId(prefix + "_" + client);
        row.setRequestId(prefix + "_REQ_" + suffix); row.setIdempotencyKey(prefix + "_IDEMP_" + suffix);
        row.setSide("BUY"); row.setOrderType("LIMIT"); row.setQuantity(new BigDecimal("10.00000000")); row.setLimitPrice(new BigDecimal("10.00000000")); row.setStatus("NEW");
        row.setFrozenCash(frozenCash); row.setFrozenQuantity(frozenQuantity); return row;
    }

    private static PaperOrderRow copy(PaperOrderRow source) {
        PaperOrderRow row = new PaperOrderRow(); row.setAccountId(source.getAccountId()); row.setInstrumentId(source.getInstrumentId()); row.setClientOrderId(source.getClientOrderId()); row.setRequestId(source.getRequestId()); row.setIdempotencyKey(source.getIdempotencyKey()); row.setSide(source.getSide()); row.setOrderType(source.getOrderType()); row.setQuantity(source.getQuantity()); row.setLimitPrice(source.getLimitPrice()); row.setStatus(source.getStatus()); row.setFrozenCash(source.getFrozenCash()); row.setFrozenQuantity(source.getFrozenQuantity()); return row;
    }

    private SqlSessionFactory createSessionFactory() throws Exception {
        UnpooledDataSource ds = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", url, user, password);
        Configuration configuration = new Configuration(new Environment("p1b", new JdbcTransactionFactory(), ds));
        configuration.addMapper(TradingAccountMapper.class); configuration.addMapper(PaperOrderMapper.class); configuration.addMapper(PaperExecutionMapper.class); configuration.addMapper(CashLedgerMapper.class); configuration.addMapper(PositionMapper.class); configuration.addMapper(TradingHaltMapper.class); configuration.addMapper(RiskSnapshotMapper.class);
        for (String resource : List.of("TradingAccountMapper.xml", "PaperOrderMapper.xml", "PaperExecutionMapper.xml", "CashLedgerMapper.xml", "PositionMapper.xml", "TradingHaltMapper.xml", "RiskSnapshotMapper.xml")) {
            try (var input = getClass().getClassLoader().getResourceAsStream("mapper/quantpf/" + resource)) {
                assertNotNull(input, resource); new XMLMapperBuilder(input, configuration, "mapper/quantpf/" + resource, configuration.getSqlFragments()).parse();
            }
        }
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private List<PaperPersistenceTransactionService.SubmissionResult> runConcurrently(ThrowingSupplier<PaperPersistenceTransactionService.SubmissionResult> a, ThrowingSupplier<PaperPersistenceTransactionService.SubmissionResult> b) throws Exception {
        return runTwo(a, b);
    }
    private List<PaperPersistenceTransactionService.SettlementResult> runConcurrently(ThrowingSettlementSupplier a, ThrowingSettlementSupplier b) throws Exception { return runTwoSettlement(a, b); }
    private interface ThrowingSupplier<T> { T get() throws Exception; }
    private interface ThrowingSettlementSupplier { PaperPersistenceTransactionService.SettlementResult get() throws Exception; }
    private List<PaperPersistenceTransactionService.SubmissionResult> runTwo(ThrowingSupplier<PaperPersistenceTransactionService.SubmissionResult> a, ThrowingSupplier<PaperPersistenceTransactionService.SubmissionResult> b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        try { List<Future<PaperPersistenceTransactionService.SubmissionResult>> futures = new ArrayList<>(); for (ThrowingSupplier<PaperPersistenceTransactionService.SubmissionResult> task : List.of(a, b)) futures.add(pool.submit(() -> { ready.countDown(); start.await(10, TimeUnit.SECONDS); return task.get(); })); assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown(); return List.of(futures.get(0).get(30, TimeUnit.SECONDS), futures.get(1).get(30, TimeUnit.SECONDS)); } finally { pool.shutdownNow(); }
    }
    private List<PaperPersistenceTransactionService.SettlementResult> runTwoSettlement(ThrowingSettlementSupplier a, ThrowingSettlementSupplier b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        try { List<Future<PaperPersistenceTransactionService.SettlementResult>> futures = new ArrayList<>(); for (ThrowingSettlementSupplier task : List.of(a, b)) futures.add(pool.submit(() -> { ready.countDown(); start.await(10, TimeUnit.SECONDS); return task.get(); })); assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown(); return List.of(futures.get(0).get(30, TimeUnit.SECONDS), futures.get(1).get(30, TimeUnit.SECONDS)); } finally { pool.shutdownNow(); }
    }

    private Connection connection() throws Exception { return DriverManager.getConnection(url, user, password); }
    private long queryLong(String sql, Object... args) throws Exception { try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); } } }
    private BigDecimal queryDecimal(String sql, Object... args) throws Exception { try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1).setScale(8); } } }
    private String queryString(String sql, Object... args) throws Exception { try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getString(1); } } }
    private static void bind(PreparedStatement ps, Object[] args) throws Exception { for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]); }
}


