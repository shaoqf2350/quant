package com.quantpf.finance;

import com.quantpf.domain.persistence.record.RiskSnapshotRow;
import com.quantpf.domain.persistence.record.TradingHaltRow;
import com.quantpf.domain.repository.RiskSnapshotRepository;
import com.quantpf.domain.repository.TradingHaltRepository;
import com.quantpf.risk.PaperRiskGate;

import java.math.BigDecimal;
import java.time.Instant;

/** Database-backed Paper risk boundary. No in-memory fallback is used on the Spring path. */
public final class PersistentPaperRiskApplicationService {
    private static final String GLOBAL_SCOPE = "GLOBAL";
    private final TradingHaltRepository haltRepository;
    private final RiskSnapshotRepository snapshotRepository;
    private final PaperRiskGate gate;

    public PersistentPaperRiskApplicationService(TradingHaltRepository haltRepository,
                                                 RiskSnapshotRepository snapshotRepository,
                                                 PaperRiskGate gate) {
        this.haltRepository = haltRepository;
        this.snapshotRepository = snapshotRepository;
        this.gate = gate;
    }

    public RiskStatusView status() {
        TradingHaltRow active = haltRepository.findActive(GLOBAL_SCOPE, null).orElse(null);
        if (active == null) return new RiskStatusView("CLEAR", null);
        return new RiskStatusView("HALTED", active.getReason());
    }

    public RiskStatusView halt(String reason) {
        String normalizedReason = reason == null || reason.isBlank() ? "Paper trading halted" : reason;
        Instant now = Instant.now();
        TradingHaltRow row = new TradingHaltRow();
        row.setScope(GLOBAL_SCOPE);
        row.setTargetId(null);
        row.setReason(normalizedReason);
        row.setHaltedBy("PAPER_API");
        row.setActive(true);
        row.setHaltedAt(now);
        haltRepository.save(row);
        gate.haltGlobal(normalizedReason);
        saveSnapshot("HALTED", normalizedReason, now);
        return new RiskStatusView("HALTED", normalizedReason);
    }

    public RiskStatusView resume(String reason) {
        String normalizedReason = reason == null || reason.isBlank() ? "Paper trading resumed" : reason;
        Instant now = Instant.now();
        haltRepository.findActive(GLOBAL_SCOPE, null)
            .ifPresent(row -> haltRepository.resume(row.getId(), now, normalizedReason));
        gate.resumeGlobal();
        saveSnapshot("CLEAR", normalizedReason, now);
        return new RiskStatusView("CLEAR", null);
    }

    public PaperRiskGate gate() { return gate; }

    private void saveSnapshot(String status, String reason, Instant at) {
        RiskSnapshotRow snapshot = new RiskSnapshotRow();
        snapshot.setAccountId(null);
        snapshot.setRiskStatus(status);
        snapshot.setRiskScore(status.equals("HALTED") ? BigDecimal.ONE : BigDecimal.ZERO);
        snapshot.setReason(reason);
        snapshot.setSnapshotAt(at);
        snapshot.setData(null);
        snapshotRepository.save(snapshot);
    }

    public record RiskStatusView(String status, String reason) {}
}
