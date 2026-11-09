package com.quantpf.risk;

import com.quantpf.domain.enums.OrderSide;
import com.quantpf.domain.value.Money;
import com.quantpf.domain.value.Quantity;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class PaperRiskGateTest {
    private RiskContext context(Instant dataAt){return new RiskContext(1L,1L,OrderSide.BUY,new Quantity(new BigDecimal("1")),new Money(new BigDecimal("10")),true,dataAt);}
    @Test void acceptsFreshOpenMarket(){PaperRiskGate gate=new PaperRiskGate(Duration.ofMinutes(1));assertTrue(gate.evaluate(context(Instant.now())).accepted());}
    @Test void rejectsGlobalHalt(){PaperRiskGate gate=new PaperRiskGate(Duration.ofMinutes(1));gate.haltGlobal("manual");RiskDecision d=gate.evaluate(context(Instant.now()));assertFalse(d.accepted());assertEquals("QPF-RISK-002",d.code());}
    @Test void rejectsStaleData(){PaperRiskGate gate=new PaperRiskGate(Duration.ofSeconds(1));RiskDecision d=gate.evaluate(context(Instant.now().minusSeconds(2)));assertFalse(d.accepted());assertEquals("QPF-MARKET-001",d.code());}
    @Test void rejectsClosedMarket(){PaperRiskGate gate=new PaperRiskGate(Duration.ofMinutes(1));RiskDecision d=gate.evaluate(new RiskContext(1L,1L,OrderSide.BUY,new Quantity(new BigDecimal("1")),new Money(new BigDecimal("10")),false,Instant.now()));assertFalse(d.accepted());assertEquals("QPF-RISK-003",d.code());}
}