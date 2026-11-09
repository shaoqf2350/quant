package com.quantpf.finance;

import com.quantpf.domain.entity.Position;
import com.quantpf.domain.entity.TradingAccount;
import com.quantpf.domain.value.Money;
import com.quantpf.domain.value.Quantity;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PaperAccountLedgerServiceTest {
    @Test void buySettlementUpdatesAccountAndPosition(){
        PaperAccountLedgerService svc=new PaperAccountLedgerService();
        TradingAccount account=new TradingAccount(1L,"PAPER-1",new Money(new BigDecimal("1000")));
        Position position=new Position(1L,1L);
        Money price=new Money(new BigDecimal("10")); Quantity qty=new Quantity(new BigDecimal("5")); Money total=svc.notional(price,qty);
        svc.reserveCash(account,total); svc.settleCash(account,total); svc.settleBuy(position,qty,price);
        assertEquals(new BigDecimal("950.00000000"),account.getAvailableCash().value());
        assertEquals(new BigDecimal("5.00000000"),position.getQuantity().value());
    }
    @Test void sellSettlementUsesFrozenPosition(){
        PaperAccountLedgerService svc=new PaperAccountLedgerService(); Position p=new Position(1L,1L); Money price=new Money(new BigDecimal("10")); Quantity qty=new Quantity(new BigDecimal("5"));
        p.addBought(qty,price); svc.reservePosition(p,new Quantity(new BigDecimal("2"))); svc.settleSell(p,new Quantity(new BigDecimal("2")),new Money(new BigDecimal("11")));
        assertEquals(new BigDecimal("3.00000000"),p.getQuantity().value()); assertEquals(new BigDecimal("3.00000000"),p.getAvailableQuantity().value());
    }
}