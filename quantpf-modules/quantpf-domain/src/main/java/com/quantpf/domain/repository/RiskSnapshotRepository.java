package com.quantpf.domain.repository;

import com.quantpf.domain.persistence.record.RiskSnapshotRow;
import java.util.Optional;

public interface RiskSnapshotRepository {
    Optional<RiskSnapshotRow> findLatestByAccountId(Long accountId);
    RiskSnapshotRow save(RiskSnapshotRow row);
}

