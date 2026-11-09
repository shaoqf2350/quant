package com.quantpf.domain.repository;

import com.quantpf.domain.persistence.record.CashLedgerRow;
import java.util.Optional;

public interface CashLedgerRepository {
    Optional<CashLedgerRow> findByIdempotencyKey(String idempotencyKey);
    CashLedgerRow save(CashLedgerRow row);
}

