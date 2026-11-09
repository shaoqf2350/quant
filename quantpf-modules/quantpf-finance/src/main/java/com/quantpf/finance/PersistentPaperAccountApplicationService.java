package com.quantpf.finance;

import com.quantpf.domain.persistence.mapper.CashLedgerMapper;
import com.quantpf.domain.persistence.mapper.TradingAccountMapper;
import com.quantpf.domain.persistence.record.CashLedgerRow;
import com.quantpf.domain.persistence.record.TradingAccountRow;

import java.math.BigDecimal;
import java.util.List;

/** Database-backed Paper account read boundary for the Spring API. */
public final class PersistentPaperAccountApplicationService {
    private final TradingAccountMapper accountMapper;
    private final CashLedgerMapper ledgerMapper;

    public PersistentPaperAccountApplicationService(TradingAccountMapper accountMapper, CashLedgerMapper ledgerMapper) {
        this.accountMapper = accountMapper;
        this.ledgerMapper = ledgerMapper;
    }

    public List<AccountView> page(int pageNum, int pageSize) {
        int safePage = Math.max(pageNum, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safeSize;
        return accountMapper.selectPage(offset, safeSize).stream()
            .map(row -> new AccountView(row.getId(), row.getAccountCode(), row.getStatus(), row.getAvailableCash(), row.getFrozenCash()))
            .toList();
    }

    public List<LedgerEntry> ledger(Long accountId) {
        if (accountMapper.selectById(accountId) == null) {
            throw new IllegalArgumentException("QPF-ACCOUNT-001: Account not found");
        }
        return ledgerMapper.selectByAccountId(accountId).stream().map(PersistentPaperAccountApplicationService::toLedgerEntry).toList();
    }

    private static LedgerEntry toLedgerEntry(CashLedgerRow row) {
        return new LedgerEntry(row.getLedgerType(), row.getAmount(), row.getBalanceAfter(), row.getReferenceId(), row.getOccurredAt());
    }

    public record AccountView(Long id, String accountCode, String status, BigDecimal availableCash, BigDecimal frozenCash) {}
    public record LedgerEntry(String type, BigDecimal amount, BigDecimal balanceAfter, String referenceId, java.time.Instant occurredAt) {}
}
