package com.quantpf.finance;

import com.quantpf.domain.persistence.mapper.PaperFollowSessionMapper;
import com.quantpf.domain.persistence.record.PaperFollowSessionRow;
import com.quantpf.domain.repository.PaperFollowSessionRepository;

import java.time.Instant;
import java.util.Objects;

public class PaperFollowSessionRepositoryAdapter implements PaperFollowSessionRepository {
    private final PaperFollowSessionMapper mapper;

    public PaperFollowSessionRepositoryAdapter(PaperFollowSessionMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override public PaperFollowSessionRow findByStartRequestId(String requestId) { return mapper.selectByStartRequestId(requestId); }
    @Override public PaperFollowSessionRow findByStopRequestId(String requestId) { return mapper.selectByStopRequestId(requestId); }
    @Override public PaperFollowSessionRow findRunning(Long accountId, Long strategyId, String mode) { return mapper.selectRunning(accountId, strategyId, mode); }
    @Override public PaperFollowSessionRow findBySessionIdForUpdate(String sessionId) { return mapper.selectBySessionIdForUpdate(sessionId); }
    @Override public PaperFollowSessionRow findRunningForUpdateNowait(Long accountId, Long strategyId, String mode) { return mapper.selectRunningForUpdateNowait(accountId, strategyId, mode); }
    @Override public int insert(PaperFollowSessionRow row) { return mapper.insert(row); }
    @Override public int stop(String sessionId, String stopRequestId, String stoppedBy, Instant stoppedAt, String stopReason) {
        return mapper.stop(sessionId, stopRequestId, stoppedBy, stoppedAt, stopReason);
    }
    @Override public String accountType(Long accountId) { return mapper.selectAccountType(accountId); }
    @Override public String accountStatus(Long accountId) { return mapper.selectAccountStatus(accountId); }
    @Override public String strategyStatus(Long strategyId) { return mapper.selectStrategyStatus(strategyId); }
}
