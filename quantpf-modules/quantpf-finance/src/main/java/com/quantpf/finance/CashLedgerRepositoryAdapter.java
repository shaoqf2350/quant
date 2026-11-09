package com.quantpf.finance;

import com.quantpf.domain.persistence.mapper.CashLedgerMapper;
import com.quantpf.domain.persistence.record.CashLedgerRow;
import com.quantpf.domain.repository.CashLedgerRepository;
import java.util.Optional;

public class CashLedgerRepositoryAdapter implements CashLedgerRepository {
    private final CashLedgerMapper mapper;
    public CashLedgerRepositoryAdapter(CashLedgerMapper mapper) { this.mapper = mapper; }
    @Override public Optional<CashLedgerRow> findByIdempotencyKey(String key) { return Optional.ofNullable(mapper.selectByIdempotencyKey(key)); }
    @Override public CashLedgerRow save(CashLedgerRow row) { mapper.insert(row); return row; }
}
