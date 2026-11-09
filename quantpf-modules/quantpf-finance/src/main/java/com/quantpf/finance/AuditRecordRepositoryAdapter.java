package com.quantpf.finance;

import com.quantpf.domain.persistence.mapper.AuditRecordMapper;
import com.quantpf.domain.persistence.record.AuditRecordRow;
import com.quantpf.domain.repository.AuditRecordRepository;
import java.util.List;
import java.util.Objects;

public class AuditRecordRepositoryAdapter implements AuditRecordRepository {
    private final AuditRecordMapper mapper;
    public AuditRecordRepositoryAdapter(AuditRecordMapper mapper) { this.mapper = Objects.requireNonNull(mapper, "mapper"); }
    @Override public List<AuditRecordRow> page(long offset, int limit, String requestId, String aggregateType, String aggregateId) {
        return mapper.selectPage(offset, limit, requestId, aggregateType, aggregateId);
    }
    @Override public int append(AuditRecordRow row) { return mapper.insert(row); }
}