package com.quantpf.domain.repository;

import com.quantpf.domain.persistence.record.TradingHaltRow;
import java.time.Instant;
import java.util.Optional;

public interface TradingHaltRepository {
    int countActive(String scope, String targetId);
    Optional<TradingHaltRow> findActive(String scope, String targetId);
    TradingHaltRow save(TradingHaltRow row);
    int resume(Long id, Instant resumedAt, String resumeReason);
}
