package com.quantpf.domain.persistence;

import com.quantpf.domain.persistence.mapper.TradingAccountMapper;
import com.quantpf.domain.persistence.record.TradingAccountRow;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TradingAccountMapperIntegrationTest {
    @Test
    void insertAndSelectTradingAccountWithinRollback() throws Exception {
        Assumptions.assumeTrue(driverAvailable(), "MySQL JDBC driver is not on the Maven test classpath");
        String user = System.getenv("QPF_DB_USER");
        String password = System.getenv("QPF_DB_PASSWORD");
        Assumptions.assumeTrue(user != null && password != null, "local DB credentials not supplied");
        String url = System.getenv().getOrDefault("QPF_JDBC_URL", "jdbc:mysql://127.0.0.1:3306/quantpf?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");

        UnpooledDataSource dataSource = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", url, user, password);
        Environment environment = new Environment("phase1b", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(TradingAccountMapper.class);
        try (InputStream mapperXml = getClass().getClassLoader().getResourceAsStream("mapper/quantpf/TradingAccountMapper.xml")) {
            assertNotNull(mapperXml);
            new XMLMapperBuilder(mapperXml, configuration, "mapper/quantpf/TradingAccountMapper.xml", configuration.getSqlFragments()).parse();
        }
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(configuration);
        try (SqlSession session = factory.openSession(false)) {
            TradingAccountMapper mapper = session.getMapper(TradingAccountMapper.class);
            TradingAccountRow row = new TradingAccountRow();
            row.setAccountCode("PHASE1B_IT_" + System.nanoTime());
            row.setAccountName("Phase 1B Integration");
            row.setAccountType("PAPER");
            row.setCurrency("CNY");
            row.setInitialCash(new BigDecimal("1000"));
            row.setAvailableCash(new BigDecimal("1000"));
            row.setFrozenCash(BigDecimal.ZERO);
            row.setStatus("ACTIVE");
            assertEquals(1, mapper.insert(row));
            assertNotNull(row.getId());
            TradingAccountRow loaded = mapper.selectById(row.getId());
            assertEquals(row.getAccountCode(), loaded.getAccountCode());
            assertEquals(new BigDecimal("1000.00000000"), loaded.getAvailableCash());
            session.rollback();
        }
    }    private static boolean driverAvailable() {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); return true; }
        catch (ClassNotFoundException e) { return false; }
    }
}