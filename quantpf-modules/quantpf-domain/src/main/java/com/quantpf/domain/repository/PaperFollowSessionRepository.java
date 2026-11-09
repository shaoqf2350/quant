package com.quantpf.domain.repository;

import com.quantpf.domain.persistence.record.PaperFollowSessionRow;

public interface PaperFollowSessionRepository {
    PaperFollowSessionRow findByStartRequestId(String startRequestId);
    PaperFollowSessionRow findByStopRequestId(String stopRequestId);
    PaperFollowSessionRow findRunning(Long accountId, Long strategyId, String mode);
    PaperFollowSessionRow findBySessionIdForUpdate(String sessionId);
    PaperFollowSessionRow findRunningForUpdateNowait(Long accountId, Long strategyId, String mode);
    int insert(PaperFollowSessionRow row);
    int stop(String sessionId, String stopRequestId, String stoppedBy, java.time.Instant stoppedAt, String stopReason);
    String accountType(Long accountId);
    String accountStatus(Long accountId);
    String strategyStatus(Long strategyId);
}
