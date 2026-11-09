package com.quantpf.finance;

import com.quantpf.domain.persistence.mapper.TradingHaltMapper;
import com.quantpf.domain.persistence.record.TradingHaltRow;
import com.quantpf.domain.repository.TradingHaltRepository;

import java.time.Instant;
import java.util.Optional;

public class TradingHaltRepositoryAdapter implements TradingHaltRepository {
    private final TradingHaltMapper mapper;
    public TradingHaltRepositoryAdapter(TradingHaltMapper mapper) { this.mapper = mapper; }
    @Override public int countActive(String scope, String targetId) { return mapper.countActive(scope, targetId); }
    @Override public Optional<TradingHaltRow> findActive(String scope, String targetId) { return Optional.ofNullable(mapper.selectActive(scope, targetId)); }
    @Override public TradingHaltRow save(TradingHaltRow row) { mapper.insert(row); return row; }
    @Override public int resume(Long id, Instant resumedAt, String resumeReason) { return mapper.resume(id, resumedAt, resumeReason); }
}
