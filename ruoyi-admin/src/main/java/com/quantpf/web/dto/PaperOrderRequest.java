package com.quantpf.web.dto;

import java.math.BigDecimal;

public record PaperOrderRequest(Long accountId, Long instrumentId, String side, String orderType, BigDecimal quantity, BigDecimal limitPrice, BigDecimal estimatedNotional, BigDecimal fillPrice, String clientOrderId, String requestId) {}