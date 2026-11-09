package com.quantpf.domain.repository;

import com.quantpf.domain.persistence.record.AuditRecordRow;
import java.util.List;

public interface AuditRecordRepository {
    List<AuditRecordRow> page(long offset, int limit, String requestId, String aggregateType, String aggregateId);
    int append(AuditRecordRow row);
}