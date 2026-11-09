package com.quantpf.domain.entity;
import com.quantpf.domain.value.Money; import com.quantpf.domain.value.Quantity; import java.time.Instant;
public record PaperExecution(Long orderId,String executionId,Quantity quantity,Money price,Money fee,Instant executedAt,String idempotencyKey) {}