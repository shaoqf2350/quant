package com.quantpf.domain;

import com.quantpf.domain.entity.TradingAccount;
import com.quantpf.domain.exception.QuantPfDomainException;
import com.quantpf.domain.value.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class DomainValueTest {
    @Test void accountFreezesAndReleasesCash() {
        TradingAccount account = new TradingAccount(1L,"PAPER-1",new Money(new BigDecimal("1000")));
        account.freezeCash(new Money(new BigDecimal("250")));
        assertEquals(new BigDecimal("750.00000000"),account.getAvailableCash().value());
        assertEquals(new BigDecimal("250.00000000"),account.getFrozenCash().value());
        account.releaseCash(new Money(new BigDecimal("250")));
        assertEquals(new BigDecimal("1000.00000000"),account.getAvailableCash().value());
    }
    @Test void insufficientCashHasStableErrorCode(){
        TradingAccount account=new TradingAccount(1L,"PAPER-1",new Money(new BigDecimal("10")));
        QuantPfDomainException ex=assertThrows(QuantPfDomainException.class,()->account.freezeCash(new Money(new BigDecimal("11"))));
        assertEquals("QPF-CASH-001",ex.code());
    }
}