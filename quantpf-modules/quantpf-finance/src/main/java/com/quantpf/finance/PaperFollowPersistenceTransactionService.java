package com.quantpf.finance;

import com.quantpf.domain.exception.QuantPfDomainException;
import com.quantpf.domain.persistence.mapper.AuditRecordMapper;
import com.quantpf.domain.persistence.mapper.PaperFollowSessionMapper;
import com.quantpf.domain.persistence.record.AuditRecordRow;
import com.quantpf.domain.persistence.record.PaperFollowSessionRow;
import com.quantpf.domain.repository.AuditRecordRepository;
import com.quantpf.domain.repository.PaperFollowSessionRepository;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Explicit single-SqlSession transaction for Follow state and Audit facts. */
public final class PaperFollowPersistenceTransactionService {
    private static final String MODE = "PAPER";
    private static final String ACTOR = "PAPER_API";

    private final SqlSessionFactory sessionFactory;
    private final AuditInsertHook auditInsertHook;

    public PaperFollowPersistenceTransactionService(SqlSessionFactory sessionFactory) {
        this(sessionFactory, () -> { });
    }

    public PaperFollowPersistenceTransactionService(SqlSessionFactory sessionFactory, AuditInsertHook auditInsertHook) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory");
        this.auditInsertHook = Objects.requireNonNull(auditInsertHook, "auditInsertHook");
    }

    public Result start(Long accountId, Long strategyId, String mode, String idempotencyKey) {
        require(accountId, "accountId");
        require(strategyId, "strategyId");
        require(idempotencyKey, "Idempotency-Key");
        if (!MODE.equals(mode)) {
            throw error("QPF-FOLLOW-004", "Paper Follow mode must be PAPER");
        }
        try (SqlSession session = sessionFactory.openSession(false)) {
            Connection connection = session.getConnection();
            begin(connection);
            try {
                PaperFollowSessionRepository follows = followRepository(session);
                AuditRecordRepository audits = auditRepository(session);
                PaperFollowSessionRow byKey = follows.findByStartRequestId(idempotencyKey);
                if (byKey != null) {
                    if (!sameStart(byKey, accountId, strategyId, mode)) {
                        throw error("QPF-IDEMP-001", "Idempotency-Key conflicts with original parameters");
                    }
                    safeRollback(connection);
                    return result(byKey, true);
                }
                validateAccountAndStrategy(follows, accountId, strategyId);
                PaperFollowSessionRow running = follows.findRunningForUpdateNowait(accountId, strategyId, mode);
                if (running != null) {
                    throw error("QPF-FOLLOW-001", "Paper Follow active session already exists");
                }
                PaperFollowSessionRow row = new PaperFollowSessionRow();
                row.setSessionId(UUID.randomUUID().toString());
                row.setAccountId(accountId);
                row.setStrategyId(strategyId);
                row.setMode(mode);
                row.setStatus("RUNNING");
                row.setStartRequestId(idempotencyKey);
                row.setStartedBy(ACTOR);
                row.setStartedAt(Instant.now());
                row.setRevision(0L);
                follows.insert(row);
                auditInsertHook.beforeInsert();
                audits.append(audit(idempotencyKey, "PAPER_FOLLOW_START", row.getSessionId(), "Follow session started"));
                commit(connection);
                return result(row, false);
            } catch (RuntimeException failure) {
                safeRollback(connection);
                if (isNoWaitLockConflict(failure)) {
                    throw error("QPF-FOLLOW-001", "QPF-FOLLOW-001: Paper Follow active session is locked or already running");
                }
                if (isDuplicateKey(failure)) {
                    return resolveStartAfterFailure(accountId, strategyId, mode, idempotencyKey, failure);
                }
                throw failure;
            }
        }
    }

    public Result stop(String sessionId, String idempotencyKey, String reason) {
        require(sessionId, "sessionId");
        require(idempotencyKey, "Idempotency-Key");
        try (SqlSession session = sessionFactory.openSession(false)) {
            Connection connection = session.getConnection();
            begin(connection);
            try {
                PaperFollowSessionRepository follows = followRepository(session);
                AuditRecordRepository audits = auditRepository(session);
                PaperFollowSessionRow byKey = follows.findByStopRequestId(idempotencyKey);
                if (byKey != null) {
                    if (!sessionId.equals(byKey.getSessionId())) {
                        throw error("QPF-IDEMP-001", "Idempotency-Key conflicts with original session");
                    }
                    safeRollback(connection);
                    return result(byKey, true);
                }
                PaperFollowSessionRow row = follows.findBySessionIdForUpdate(sessionId);
                if (row == null) {
                    throw error("QPF-FOLLOW-002", "Paper Follow session not found");
                }
                if ("STOPPED".equals(row.getStatus())) {
                    safeRollback(connection);
                    return result(row, false);
                }
                String effectiveReason = reason == null || reason.isBlank() ? "Stopped by Paper API" : reason;
                Instant stoppedAt = Instant.now();
                int updated = follows.stop(sessionId, idempotencyKey, ACTOR, stoppedAt, effectiveReason);
                if (updated != 1) {
                    throw error("QPF-FOLLOW-003", "Invalid Paper Follow state transition");
                }
                row.setStatus("STOPPED");
                row.setStopRequestId(idempotencyKey);
                row.setStoppedBy(ACTOR);
                row.setStoppedAt(stoppedAt);
                row.setStopReason(effectiveReason);
                row.setRevision((row.getRevision() == null ? 0L : row.getRevision()) + 1L);
                auditInsertHook.beforeInsert();
                audits.append(audit(idempotencyKey, "PAPER_FOLLOW_STOP", row.getSessionId(), effectiveReason));
                commit(connection);
                return result(row, false);
            } catch (RuntimeException failure) {
                safeRollback(connection);
                if (isDuplicateKey(failure)) {
                    return resolveStopAfterFailure(sessionId, idempotencyKey, failure);
                }
                throw failure;
            }
        }
    }

    private static void begin(Connection connection) {
        try {
            connection.setAutoCommit(false);
        } catch (SQLException failure) {
            throw new IllegalStateException("Unable to begin Paper Follow transaction", failure);
        }
    }

    private static void commit(Connection connection) {
        try {
            connection.commit();
        } catch (SQLException failure) {
            throw new IllegalStateException("Unable to commit Paper Follow transaction", failure);
        }
    }

    private static void safeRollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original failure; rollback diagnostics belong in the command log.
        }
    }

    private static PaperFollowSessionRepository followRepository(SqlSession session) {
        PaperFollowSessionMapper mapper = session.getMapper(PaperFollowSessionMapper.class);
        return new PaperFollowSessionRepositoryAdapter(mapper);
    }

    private static AuditRecordRepository auditRepository(SqlSession session) {
        AuditRecordMapper mapper = session.getMapper(AuditRecordMapper.class);
        return new AuditRecordRepositoryAdapter(mapper);
    }

    private static boolean isNoWaitLockConflict(RuntimeException failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sql && sql.getErrorCode() == 3572) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("nowait") && normalized.contains("lock")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isDuplicateKey(RuntimeException failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sql && "23000".equals(sql.getSQLState())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("duplicate")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Result resolveStartAfterFailure(Long accountId, Long strategyId, String mode, String key, RuntimeException failure) {
        try (SqlSession session = sessionFactory.openSession(true)) {
            PaperFollowSessionRepository follows = followRepository(session);
            PaperFollowSessionRow byKey = follows.findByStartRequestId(key);
            if (byKey != null) {
                if (sameStart(byKey, accountId, strategyId, mode)) {
                    return result(byKey, true);
                }
                throw error("QPF-IDEMP-001", "Idempotency-Key conflicts with original parameters");
            }
            PaperFollowSessionRow running = follows.findRunningForUpdateNowait(accountId, strategyId, mode);
            if (running != null) {
                throw error("QPF-FOLLOW-001", "Paper Follow active session already exists");
            }
        }
        throw failure;
    }

    private Result resolveStopAfterFailure(String sessionId, String key, RuntimeException failure) {
        try (SqlSession session = sessionFactory.openSession(true)) {
            PaperFollowSessionRepository follows = followRepository(session);
            PaperFollowSessionRow row = follows.findByStopRequestId(key);
            if (row != null && sessionId.equals(row.getSessionId())) {
                return result(row, true);
            }
        }
        throw failure;
    }

    private static void validateAccountAndStrategy(PaperFollowSessionRepository follows, Long accountId, Long strategyId) {
        if (!"PAPER".equals(follows.accountType(accountId)) || !"ACTIVE".equals(follows.accountStatus(accountId))) {
            throw error("QPF-FOLLOW-003", "Paper account is missing or unavailable");
        }
        if (!"ACTIVE".equals(follows.strategyStatus(strategyId))) {
            throw error("QPF-FOLLOW-003", "Strategy must be ACTIVE");
        }
    }

    private static boolean sameStart(PaperFollowSessionRow row, Long accountId, Long strategyId, String mode) {
        return Objects.equals(row.getAccountId(), accountId)
            && Objects.equals(row.getStrategyId(), strategyId)
            && Objects.equals(row.getMode(), mode);
    }

    private static AuditRecordRow audit(String requestId, String action, String aggregateId, String detail) {
        AuditRecordRow row = new AuditRecordRow();
        row.setRequestId(requestId);
        row.setActorId(ACTOR);
        row.setAction(action);
        row.setAggregateType("PAPER_FOLLOW");
        row.setAggregateId(aggregateId);
        row.setResult("SUCCESS");
        row.setDetail("{\"message\":\"Paper Follow state changed\"}");
        row.setOccurredAt(Instant.now());
        return row;
    }

    private static Result result(PaperFollowSessionRow row, boolean idempotent) {
        return new Result(row.getSessionId(), row.getAccountId(), row.getStrategyId(), row.getMode(),
            row.getStatus(), row.getStartedAt(), row.getStoppedAt(), idempotent);
    }

    private static void require(Object value, String name) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            throw error("QPF-FOLLOW-004", name + " is required");
        }
    }

    private static QuantPfDomainException error(String code, String message) {
        return new QuantPfDomainException(code, message);
    }

    @FunctionalInterface
    public interface AuditInsertHook {
        void beforeInsert();
    }

    public record Result(String sessionId, Long accountId, Long strategyId, String mode, String status,
                         Instant startedAt, Instant stoppedAt, boolean idempotent) {
    }
}


