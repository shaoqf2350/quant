package com.quantpf.execution;
import com.quantpf.domain.entity.PaperExecution;import com.quantpf.domain.entity.PaperOrder;import java.math.BigDecimal;import java.time.Instant;
public interface PaperBrokerAdapter { PaperExecution submit(PaperOrder order,BigDecimal executionPrice,Instant now); }