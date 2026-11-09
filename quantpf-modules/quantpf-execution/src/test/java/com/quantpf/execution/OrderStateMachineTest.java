package com.quantpf.execution;

import com.quantpf.domain.entity.PaperOrder;
import com.quantpf.domain.enums.*;
import com.quantpf.domain.exception.QuantPfDomainException;
import com.quantpf.domain.value.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {
    private PaperOrder order(){return new PaperOrder(1L,1L,1L,new ClientOrderId("client-1"),new RequestId("request-1"),new OrderIdempotencyKey("idem-1"),OrderSide.BUY,OrderType.LIMIT,new Quantity(new BigDecimal("2")),new Money(new BigDecimal("10")),Instant.now());}
    @Test void validTransitionsReachFilled(){PaperOrder o=order();OrderStateMachine sm=new OrderStateMachine();sm.transition(o,OrderStatus.ACCEPTED);sm.transition(o,OrderStatus.FILLED);assertEquals(OrderStatus.FILLED,o.getStatus());}
    @Test void invalidTransitionIsRejected(){PaperOrder o=order();OrderStateMachine sm=new OrderStateMachine();QuantPfDomainException ex=assertThrows(QuantPfDomainException.class,()->sm.transition(o,OrderStatus.FILLED));assertEquals("QPF-ORDER-001",ex.code());}
    @Test void inMemoryBrokerFillsWithoutExternalConnection(){PaperOrder o=order();InMemoryPaperBrokerAdapter broker=new InMemoryPaperBrokerAdapter(new OrderStateMachine());var fill=broker.submit(o,new BigDecimal("10.25"),Instant.parse("2026-10-06T00:00:00Z"));assertEquals(OrderStatus.FILLED,o.getStatus());assertEquals(new BigDecimal("2.00000000"),fill.quantity().value());}
}