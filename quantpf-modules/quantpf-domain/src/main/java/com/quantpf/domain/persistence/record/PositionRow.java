package com.quantpf.domain.persistence.record;

import java.math.BigDecimal;

public class PositionRow {
    private Long id;
    private Long accountId;
    private Long instrumentId;
    private BigDecimal quantity;
    private BigDecimal availableQuantity;
    private BigDecimal frozenQuantity;
    private BigDecimal averageCost;
    private BigDecimal realizedPnl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getInstrumentId() { return instrumentId; }
    public void setInstrumentId(Long instrumentId) { this.instrumentId = instrumentId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(BigDecimal availableQuantity) { this.availableQuantity = availableQuantity; }
    public BigDecimal getFrozenQuantity() { return frozenQuantity; }
    public void setFrozenQuantity(BigDecimal frozenQuantity) { this.frozenQuantity = frozenQuantity; }
    public BigDecimal getAverageCost() { return averageCost; }
    public void setAverageCost(BigDecimal averageCost) { this.averageCost = averageCost; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }
}
