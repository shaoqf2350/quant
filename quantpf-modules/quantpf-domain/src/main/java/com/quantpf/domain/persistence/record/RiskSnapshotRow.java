package com.quantpf.domain.persistence.record;

import java.math.BigDecimal;
import java.time.Instant;

public class RiskSnapshotRow {
    private Long id;
    private Long accountId;
    private String riskStatus;
    private BigDecimal riskScore;
    private String reason;
    private Instant snapshotAt;
    private String data;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getRiskStatus() { return riskStatus; }
    public void setRiskStatus(String riskStatus) { this.riskStatus = riskStatus; }
    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getSnapshotAt() { return snapshotAt; }
    public void setSnapshotAt(Instant snapshotAt) { this.snapshotAt = snapshotAt; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
