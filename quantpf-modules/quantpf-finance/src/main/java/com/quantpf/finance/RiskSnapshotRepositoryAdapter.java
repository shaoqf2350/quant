package com.quantpf.finance;

import com.quantpf.domain.persistence.mapper.RiskSnapshotMapper;
import com.quantpf.domain.persistence.record.RiskSnapshotRow;
import com.quantpf.domain.repository.RiskSnapshotRepository;
import java.util.Optional;

public class RiskSnapshotRepositoryAdapter implements RiskSnapshotRepository {
    private final RiskSnapshotMapper mapper;
    public RiskSnapshotRepositoryAdapter(RiskSnapshotMapper mapper) { this.mapper = mapper; }
    @Override public Optional<RiskSnapshotRow> findLatestByAccountId(Long accountId) { return Optional.ofNullable(mapper.selectLatestByAccountId(accountId)); }
    @Override public RiskSnapshotRow save(RiskSnapshotRow row) { mapper.insert(row); return row; }
}
