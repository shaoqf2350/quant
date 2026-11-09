package com.quantpf.finance;

import com.quantpf.domain.persistence.record.AuditRecordRow;
import com.quantpf.domain.repository.AuditRecordRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Database-backed Paper audit read boundary. No in-memory fallback on the Spring path. */
public final class PersistentAuditApplicationService {
    private final AuditRecordRepository repository;

    public PersistentAuditApplicationService(AuditRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<AuditView> page(int pageNum, int pageSize) {
        return page(pageNum, pageSize, null, null, null);
    }

    public List<AuditView> page(int pageNum, int pageSize, String requestId, String aggregateType, String aggregateId) {
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        long safePageNum = Math.max((long) pageNum, 1L);
        long offset = (safePageNum - 1L) * safePageSize;
        List<AuditRecordRow> rows = Objects.requireNonNull(
            repository.page(offset, safePageSize, normalize(requestId), normalize(aggregateType), normalize(aggregateId)),
            "Audit repository returned null");
        return rows.stream().map(PersistentAuditApplicationService::toView).toList();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static AuditView toView(AuditRecordRow row) {
        return new AuditView(row.getId(), row.getRequestId(), row.getActorId(), row.getAction(),
            row.getAggregateType(), row.getAggregateId(), row.getResult(), row.getDetail(), row.getOccurredAt());
    }

    public record AuditView(Long id, String requestId, String actorId, String action,
                            String aggregateType, String aggregateId, String result, String detail, Instant occurredAt) {}
}