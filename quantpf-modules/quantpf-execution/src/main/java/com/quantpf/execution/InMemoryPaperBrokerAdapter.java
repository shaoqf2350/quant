package com.quantpf.execution;
import com.quantpf.domain.entity.*;import com.quantpf.domain.value.*;import java.math.BigDecimal;import java.time.Instant;import java.util.UUID;
public class InMemoryPaperBrokerAdapter implements PaperBrokerAdapter{
 private final OrderStateMachine stateMachine;
 public InMemoryPaperBrokerAdapter(OrderStateMachine stateMachine){this.stateMachine=stateMachine;}
 public PaperExecution submit(PaperOrder order,BigDecimal price,Instant now){stateMachine.transition(order,com.quantpf.domain.enums.OrderStatus.ACCEPTED);var execution=new PaperExecution(order.getId(),UUID.randomUUID().toString(),order.getQuantity(),new Money(price),Money.zero(),now,"execution:"+order.getIdempotencyKey().value());stateMachine.transition(order,com.quantpf.domain.enums.OrderStatus.FILLED);return execution;}
}