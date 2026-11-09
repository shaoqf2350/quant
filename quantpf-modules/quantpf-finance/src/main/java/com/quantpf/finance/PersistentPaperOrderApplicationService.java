package com.quantpf.finance;

import com.quantpf.domain.persistence.mapper.PaperOrderMapper;
import com.quantpf.domain.persistence.record.PaperOrderRow;
import com.quantpf.risk.PaperRiskGate;
import com.quantpf.risk.RiskContext;
import com.quantpf.risk.RiskDecision;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Database-backed Paper order command/query boundary. */
public final class PersistentPaperOrderApplicationService {
    private final PaperOrderMapper orderMapper;
    private final PaperPersistenceTransactionService transactionService;
    private final PaperRiskGate riskGate;

    public PersistentPaperOrderApplicationService(PaperOrderMapper orderMapper,
                                                  PaperPersistenceTransactionService transactionService,
                                                  PaperRiskGate riskGate) {
        this.orderMapper = orderMapper;
        this.transactionService = transactionService;
        this.riskGate = riskGate;
    }

    public List<OrderView> page(int pageNum, int pageSize) {
        int safePage = Math.max(pageNum, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safeSize;
        return orderMapper.selectPage(offset, safeSize).stream().map(PersistentPaperOrderApplicationService::toView).toList();
    }

    public OrderView submit(SubmitOrderCommand command) {
        if (command.estimatedNotional() == null || command.estimatedNotional().signum() <= 0) {
            throw new IllegalArgumentException("estimatedNotional must be positive");
        }
        if (command.fillPrice() == null || command.fillPrice().signum() <= 0) {
            throw new IllegalArgumentException("fillPrice must be positive");
        }
        if (command.quantity() == null || command.quantity().signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        RiskDecision risk = riskGate.evaluate(new RiskContext(
            command.accountId(), command.instrumentId(),
            com.quantpf.domain.enums.OrderSide.valueOf(command.side()),
            new com.quantpf.domain.value.Quantity(command.quantity()),
            new com.quantpf.domain.value.Money(command.estimatedNotional()),
            true, Instant.now()));
        if (!risk.accepted()) {
            throw new com.quantpf.domain.exception.QuantPfDomainException(risk.code(), risk.reason());
        }

        PaperOrderRow requested = new PaperOrderRow();
        requested.setAccountId(command.accountId());
        requested.setInstrumentId(command.instrumentId());
        requested.setClientOrderId(command.clientOrderId());
        requested.setRequestId(command.requestId());
        requested.setIdempotencyKey(command.idempotencyKey());
        requested.setSide(command.side());
        requested.setOrderType(command.orderType());
        requested.setQuantity(command.quantity());
        requested.setLimitPrice(command.limitPrice());
        requested.setStatus("NEW");
        requested.setFrozenCash("BUY".equals(command.side()) ? command.estimatedNotional() : BigDecimal.ZERO);
        requested.setFrozenQuantity("SELL".equals(command.side()) ? command.quantity() : BigDecimal.ZERO);

        PaperPersistenceTransactionService.SubmissionResult submission = transactionService.submit(requested);
        PaperOrderRow persisted = orderMapper.selectById(submission.orderId());
        if (persisted == null) throw new IllegalStateException("QPF-ORDER-001: persisted order not found");
        if (!submission.duplicate()) {
            String executionId = "paper-execution-" + UUID.randomUUID();
            transactionService.settle(new PaperPersistenceTransactionService.SettlementCommand(
                submission.orderId(), executionId, "execution-idempotency-" + executionId,
                command.quantity(), command.fillPrice(), BigDecimal.ZERO, Instant.now(), "ledger-idempotency-" + executionId, false));
            persisted = orderMapper.selectById(submission.orderId());
        }
        return toView(persisted);
    }

    public OrderView cancel(Long orderId, String reason) {
        transactionService.cancel(orderId, reason == null ? "Canceled by Paper API" : reason);
        PaperOrderRow persisted = orderMapper.selectById(orderId);
        if (persisted == null) throw new IllegalStateException("QPF-ORDER-001: persisted order not found");
        return toView(persisted);
    }
    private static OrderView toView(PaperOrderRow row) {
        return new OrderView(row.getId(), row.getAccountId(), row.getInstrumentId(), row.getClientOrderId(), row.getStatus(), row.getSide(), row.getOrderType(), row.getQuantity(), row.getRejectReason());
    }

    public record SubmitOrderCommand(Long accountId, Long instrumentId, String clientOrderId, String requestId, String idempotencyKey, String side, String orderType, BigDecimal quantity, BigDecimal limitPrice, BigDecimal estimatedNotional, BigDecimal fillPrice) {}
    public record OrderView(Long id, Long accountId, Long instrumentId, String clientOrderId, String status, String side, String orderType, BigDecimal quantity, String rejectReason) {}
}

