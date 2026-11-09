package com.quantpf.web.config;

import com.quantpf.domain.persistence.mapper.AuditRecordMapper;
import com.quantpf.domain.persistence.mapper.CashLedgerMapper;
import com.quantpf.domain.persistence.mapper.JobExecutionAuditMapper;
import com.quantpf.domain.persistence.mapper.PaperOrderMapper;
import com.quantpf.domain.persistence.mapper.PaperFollowSessionMapper;
import com.quantpf.domain.persistence.mapper.TradingAccountMapper;
import com.quantpf.domain.persistence.mapper.TradingHaltMapper;
import com.quantpf.domain.persistence.mapper.RiskSnapshotMapper;
import com.quantpf.domain.repository.AuditRecordRepository;
import com.quantpf.domain.repository.JobExecutionAuditRepository;
import com.quantpf.domain.repository.RiskSnapshotRepository;
import com.quantpf.domain.repository.TradingHaltRepository;
import com.quantpf.finance.AuditRecordRepositoryAdapter;
import com.quantpf.finance.JobExecutionAuditRepositoryAdapter;
import com.quantpf.finance.PaperPersistenceTransactionService;
import com.quantpf.finance.PersistentAuditApplicationService;
import com.quantpf.finance.PersistentJobExecutionApplicationService;
import com.quantpf.finance.PersistentPaperAccountApplicationService;
import com.quantpf.finance.PersistentPaperOrderApplicationService;
import com.quantpf.finance.PaperFollowPersistenceTransactionService;
import com.quantpf.finance.PersistentPaperFollowApplicationService;
import com.quantpf.finance.PersistentPaperRiskApplicationService;
import com.quantpf.finance.RiskSnapshotRepositoryAdapter;
import com.quantpf.finance.TradingHaltRepositoryAdapter;
import com.quantpf.risk.PaperRiskGate;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * Controlled Spring wiring for the Paper persistence path.
 * No Redis, broker, scheduler or live gateway bean is created here.
 */
@Configuration(proxyBeanMethods = false)
@MapperScan(basePackages = "com.quantpf.domain.persistence.mapper")
public class QuantPfPaperRuntimeConfiguration {
    @Bean
    public PaperRiskGate paperRiskGate() {
        return new PaperRiskGate(Duration.ofMinutes(1));
    }

    @Bean
    public TradingHaltRepository tradingHaltRepository(TradingHaltMapper mapper) {
        return new TradingHaltRepositoryAdapter(mapper);
    }

    @Bean
    public RiskSnapshotRepository riskSnapshotRepository(RiskSnapshotMapper mapper) {
        return new RiskSnapshotRepositoryAdapter(mapper);
    }

    @Bean
    public PersistentPaperRiskApplicationService persistentPaperRiskApplicationService(
        TradingHaltRepository haltRepository, RiskSnapshotRepository snapshotRepository, PaperRiskGate gate) {
        return new PersistentPaperRiskApplicationService(haltRepository, snapshotRepository, gate);
    }

    @Bean
    public PaperPersistenceTransactionService paperPersistenceTransactionService(SqlSessionFactory sqlSessionFactory) {
        return new PaperPersistenceTransactionService(sqlSessionFactory);
    }

    @Bean
    public PersistentPaperAccountApplicationService persistentPaperAccountApplicationService(
        TradingAccountMapper accountMapper, CashLedgerMapper ledgerMapper) {
        return new PersistentPaperAccountApplicationService(accountMapper, ledgerMapper);
    }

    @Bean
    public PersistentPaperOrderApplicationService persistentPaperOrderApplicationService(
        PaperOrderMapper orderMapper, PaperPersistenceTransactionService transactionService,
        PaperRiskGate riskGate) {
        return new PersistentPaperOrderApplicationService(orderMapper, transactionService, riskGate);
    }

    @Bean
    public PaperFollowPersistenceTransactionService paperFollowPersistenceTransactionService(SqlSessionFactory sqlSessionFactory) {
        return new PaperFollowPersistenceTransactionService(sqlSessionFactory);
    }

    @Bean
    public PersistentPaperFollowApplicationService persistentPaperFollowApplicationService(
        PaperFollowPersistenceTransactionService transactionService) {
        return new PersistentPaperFollowApplicationService(transactionService);
    }
    @Bean
    public AuditRecordRepository auditRecordRepository(AuditRecordMapper mapper) {
        return new AuditRecordRepositoryAdapter(mapper);
    }

    @Bean
    public PersistentAuditApplicationService persistentAuditApplicationService(AuditRecordRepository repository) {
        return new PersistentAuditApplicationService(repository);
    }

    @Bean
    public JobExecutionAuditRepository jobExecutionAuditRepository(JobExecutionAuditMapper mapper) {
        return new JobExecutionAuditRepositoryAdapter(mapper);
    }

    @Bean
    public PersistentJobExecutionApplicationService persistentJobExecutionApplicationService(JobExecutionAuditRepository repository) {
        return new PersistentJobExecutionApplicationService(repository);
    }

    @Bean
    @ConditionalOnMissingBean(PlatformTransactionManager.class)
    public PlatformTransactionManager quantPfTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}


