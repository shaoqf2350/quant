package com.quantpf.domain.entity;

import com.quantpf.domain.enums.AccountStatus;
import com.quantpf.domain.enums.AccountType;
import com.quantpf.domain.exception.QuantPfDomainException;
import com.quantpf.domain.value.Money;
import java.util.Objects;

public class TradingAccount {
    private final Long id;
    private final String accountCode;
    private final String accountName;
    private final AccountType accountType = AccountType.PAPER;
    private AccountStatus status = AccountStatus.ACTIVE;
    private final Money initialCash;
    private Money availableCash;
    private Money frozenCash = Money.zero();
    public TradingAccount(Long id, String accountCode, Money initialCash) {
        if (initialCash.isNegative()) throw new IllegalArgumentException("initial cash must not be negative");
        this.id=id; this.accountCode=Objects.requireNonNull(accountCode); this.accountName=accountCode; this.initialCash=initialCash; this.availableCash=initialCash;
    }
    public Long getId(){return id;} public String getAccountCode(){return accountCode;} public String getAccountName(){return accountName;} public AccountType getAccountType(){return accountType;}
    public AccountStatus getStatus(){return status;} public Money getInitialCash(){return initialCash;} public Money getAvailableCash(){return availableCash;} public Money getFrozenCash(){return frozenCash;}
    public void restoreBalances(Money available, Money frozen) { this.availableCash=available; this.frozenCash=frozen; }
    public void halt(){status=AccountStatus.HALTED;} public void activate(){if(status!=AccountStatus.CLOSED)status=AccountStatus.ACTIVE;}
    public void freezeCash(Money amount){active(); positive(amount); if(availableCash.compareTo(amount)<0)throw new QuantPfDomainException("QPF-CASH-001","Insufficient cash"); availableCash=availableCash.subtract(amount); frozenCash=frozenCash.add(amount);}
    public void releaseCash(Money amount){positive(amount); if(frozenCash.compareTo(amount)<0)throw new QuantPfDomainException("QPF-CASH-001","Frozen cash is insufficient"); frozenCash=frozenCash.subtract(amount); availableCash=availableCash.add(amount);}
    public void settleFrozenCash(Money amount){positive(amount); if(frozenCash.compareTo(amount)<0)throw new QuantPfDomainException("QPF-CASH-001","Frozen cash is insufficient"); frozenCash=frozenCash.subtract(amount);}
    private void active(){if(status!=AccountStatus.ACTIVE)throw new QuantPfDomainException("QPF-RISK-002","Account is halted or closed");}
    private static void positive(Money a){if(!a.isPositive())throw new IllegalArgumentException("amount must be positive");}
}