package com.quantpf.execution;
import com.quantpf.domain.entity.*;import com.quantpf.domain.value.*;import com.quantpf.domain.enums.*;import com.quantpf.risk.*;import java.math.BigDecimal;import java.time.Instant;
public class PaperOrderService{
 private final RiskGate riskGate;private final PaperBrokerAdapter broker;
 public PaperOrderService(RiskGate riskGate,PaperBrokerAdapter broker){this.riskGate=riskGate;this.broker=broker;}
 public PaperExecution submit(PaperOrder order,Money estimatedNotional,boolean marketOpen,Instant marketDataAt,BigDecimal fillPrice,Instant now){RiskDecision d=riskGate.evaluate(new RiskContext(order.getAccountId(),order.getInstrumentId(),order.getSide(),order.getQuantity(),estimatedNotional,marketOpen,marketDataAt));if(!d.accepted()){order.reject(d.code()+":"+d.reason());throw new com.quantpf.domain.exception.QuantPfDomainException(d.code(),d.reason());}return broker.submit(order,fillPrice,now);}
}