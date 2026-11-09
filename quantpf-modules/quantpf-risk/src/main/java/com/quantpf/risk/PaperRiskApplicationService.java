package com.quantpf.risk;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class PaperRiskApplicationService {
    private final PaperRiskGate gate = new PaperRiskGate(java.time.Duration.ofMinutes(1));
    private final AtomicReference<String> reason = new AtomicReference<>();
    public RiskStatusView status() { return new RiskStatusView(reason.get() == null ? "CLEAR" : "HALTED", reason.get()); }
    public RiskStatusView halt(String reasonText) { gate.haltGlobal(reasonText); reason.set(reasonText); return status(); }
    public RiskStatusView resume(String reasonText) { gate.resumeGlobal(); reason.set(null); return status(); }
    public PaperRiskGate gate() { return gate; }
    public record RiskStatusView(String status, String reason) {}
}