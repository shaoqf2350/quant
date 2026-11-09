package com.quantpf.finance;

import java.math.BigDecimal;

/** Spring-facing Paper Follow application boundary. Transaction ownership stays in the explicit MyBatis service. */
public final class PersistentPaperFollowApplicationService {
    private final PaperFollowPersistenceTransactionService transactionService;
    public PersistentPaperFollowApplicationService(PaperFollowPersistenceTransactionService transactionService) { this.transactionService = transactionService; }
    public PaperFollowPersistenceTransactionService.Result start(Long accountId, Long strategyId, String mode, String idempotencyKey) { return transactionService.start(accountId, strategyId, mode, idempotencyKey); }
    public PaperFollowPersistenceTransactionService.Result stop(String sessionId, String idempotencyKey, String reason) { return transactionService.stop(sessionId, idempotencyKey, reason); }
}