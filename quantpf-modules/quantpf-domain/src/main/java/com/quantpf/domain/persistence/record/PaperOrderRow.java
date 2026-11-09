package com.quantpf.domain.persistence.record;

import java.math.BigDecimal;
import java.time.Instant;

public class PaperOrderRow {
    private Long id;
    private Long accountId;
    private Long instrumentId;
    private String clientOrderId;
    private String requestId;
    private String idempotencyKey;
    private String side;
    private String orderType;
    private BigDecimal quantity;
    private BigDecimal limitPrice;
    private String status;
    private String rejectReason;
    private BigDecimal frozenCash;
    private BigDecimal frozenQuantity;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getInstrumentId() { return instrumentId; }
    public void setInstrumentId(Long instrumentId) { this.instrumentId = instrumentId; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public void setLimitPrice(BigDecimal limitPrice) { this.limitPrice = limitPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public BigDecimal getFrozenCash() { return frozenCash; }
    public void setFrozenCash(BigDecimal frozenCash) { this.frozenCash = frozenCash; }
    public BigDecimal getFrozenQuantity() { return frozenQuantity; }
    public void setFrozenQuantity(BigDecimal frozenQuantity) { this.frozenQuantity = frozenQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
