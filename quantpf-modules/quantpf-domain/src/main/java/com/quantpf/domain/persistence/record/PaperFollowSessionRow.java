package com.quantpf.domain.persistence.record;

import java.time.Instant;

public class PaperFollowSessionRow {
    private Long id;
    private String sessionId;
    private Long accountId;
    private Long strategyId;
    private String mode;
    private String status;
    private String startRequestId;
    private String stopRequestId;
    private String startedBy;
    private String stoppedBy;
    private Instant startedAt;
    private Instant stoppedAt;
    private String stopReason;
    private Long revision;
    private Instant createdAt;
    private Instant updatedAt;
    private String activeSlot;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getStrategyId() { return strategyId; }
    public void setStrategyId(Long strategyId) { this.strategyId = strategyId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStartRequestId() { return startRequestId; }
    public void setStartRequestId(String startRequestId) { this.startRequestId = startRequestId; }
    public String getStopRequestId() { return stopRequestId; }
    public void setStopRequestId(String stopRequestId) { this.stopRequestId = stopRequestId; }
    public String getStartedBy() { return startedBy; }
    public void setStartedBy(String startedBy) { this.startedBy = startedBy; }
    public String getStoppedBy() { return stoppedBy; }
    public void setStoppedBy(String stoppedBy) { this.stoppedBy = stoppedBy; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(Instant stoppedAt) { this.stoppedAt = stoppedAt; }
    public String getStopReason() { return stopReason; }
    public void setStopReason(String stopReason) { this.stopReason = stopReason; }
    public Long getRevision() { return revision; }
    public void setRevision(Long revision) { this.revision = revision; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getActiveSlot() { return activeSlot; }
    public void setActiveSlot(String activeSlot) { this.activeSlot = activeSlot; }
}