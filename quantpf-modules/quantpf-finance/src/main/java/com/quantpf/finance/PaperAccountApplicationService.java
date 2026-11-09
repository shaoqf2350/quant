package com.quantpf.finance;

import com.quantpf.domain.entity.TradingAccount;
import com.quantpf.domain.value.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory application boundary for the first Paper API slice. */
public class PaperAccountApplicationService {
    private final Map<Long, TradingAccount> accounts = new ConcurrentHashMap<>();
    private final Map<Long, List<LedgerEntry>> ledgers = new ConcurrentHashMap<>();

    public PaperAccountApplicationService() {
        TradingAccount demo = new TradingAccount(1L, "PAPER-DEMO", new Money(new BigDecimal("100000")));
        accounts.put(demo.getId(), demo);
        ledgers.put(demo.getId(), new java.util.concurrent.CopyOnWriteArrayList<>());
    }

    public List<AccountView> page(int pageNum, int pageSize) {
        return accounts.values().stream().skip((long) Math.max(pageNum - 1, 0) * pageSize).limit(pageSize)
            .map(a -> new AccountView(a.getId(), a.getAccountCode(), a.getStatus().name(), a.getAvailableCash().value(), a.getFrozenCash().value()))
            .toList();
    }

    public List<LedgerEntry> ledger(Long accountId) {
        require(accountId);
        return List.copyOf(ledgers.getOrDefault(accountId, List.of()));
    }

    public TradingAccount require(Long accountId) {
        TradingAccount account = accounts.get(accountId);
        if (account == null) throw new IllegalArgumentException("QPF-ACCOUNT-001: Account not found");
        return account;
    }

    public record AccountView(Long id, String accountCode, String status, BigDecimal availableCash, BigDecimal frozenCash) {}
    public record LedgerEntry(String type, BigDecimal amount, String referenceId) {}
}