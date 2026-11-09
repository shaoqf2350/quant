package com.quantpf.domain.persistence.record;

import java.time.Instant;

public class TradingHaltRow {
    private Long id;
    private String scope;
    private String targetId;
    private String reason;
    private String haltedBy;
    private Boolean active;
    private Instant haltedAt;
    private Instant resumedAt;
    private String resumeReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getHaltedBy() { return haltedBy; }
    public void setHaltedBy(String haltedBy) { this.haltedBy = haltedBy; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Instant getHaltedAt() { return haltedAt; }
    public void setHaltedAt(Instant haltedAt) { this.haltedAt = haltedAt; }
    public Instant getResumedAt() { return resumedAt; }
    public void setResumedAt(Instant resumedAt) { this.resumedAt = resumedAt; }
    public String getResumeReason() { return resumeReason; }
    public void setResumeReason(String resumeReason) { this.resumeReason = resumeReason; }
}
