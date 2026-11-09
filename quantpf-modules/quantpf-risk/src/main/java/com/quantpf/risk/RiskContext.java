package com.quantpf.risk;
import com.quantpf.domain.enums.OrderSide;import com.quantpf.domain.value.Money;import com.quantpf.domain.value.Quantity;import java.time.Instant;
public record RiskContext(Long accountId,Long instrumentId,OrderSide side,Quantity quantity,Money estimatedNotional,boolean marketOpen,Instant marketDataAt){ }