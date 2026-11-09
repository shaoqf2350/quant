package com.quantpf.execution;

import com.quantpf.domain.entity.PaperOrder;
import com.quantpf.domain.enums.OrderSide;
import com.quantpf.domain.enums.OrderStatus;
import com.quantpf.domain.enums.OrderType;
import com.quantpf.domain.value.ClientOrderId;
import com.quantpf.domain.value.Money;
import com.quantpf.domain.value.OrderIdempotencyKey;
import com.quantpf.domain.value.Quantity;
import com.quantpf.domain.value.RequestId;
import com.quantpf.risk.PaperRiskGate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory Paper order application boundary. */
public class PaperOrderApplicationService {
    private final Map<Long, PaperOrder> orders = new ConcurrentHashMap<>();
    private final Map<String, Long> idempotency = new ConcurrentHashMap<>();
    private final PaperRiskGate riskGate;
    private final PaperBrokerAdapter broker;
    private final OrderStateMachine stateMachine;

    public PaperOrderApplicationService(PaperRiskGate riskGate) {
        this.riskGate = riskGate;
        this.stateMachine = new OrderStateMachine();
        this.broker = new InMemoryPaperBrokerAdapter(stateMachine);
    }

    public List<OrderView> page(int pageNum, int pageSize) {
        return orders.values().stream().skip((long) Math.max(pageNum - 1, 0) * pageSize).limit(pageSize)
            .map(o -> new OrderView(o.getId(), o.getAccountId(), o.getInstrumentId(), o.getClientOrderId().value(), o.getStatus().name(), o.getSide().name(), o.getOrderType().name(), o.getQuantity().value(), o.getRejectReason()))
            .toList();
    }

    public OrderView submit(SubmitOrderCommand command) {
        Long existing = idempotency.putIfAbsent(command.idempotencyKey(), command.id());
        if (existing != null) throw new IllegalStateException("QPF-IDEMP-001: Idempotency key conflict");
        PaperOrder order = new PaperOrder(command.id(), command.accountId(), command.instrumentId(), new ClientOrderId(command.clientOrderId()), new RequestId(command.requestId()), new OrderIdempotencyKey(command.idempotencyKey()), command.side(), command.orderType(), new Quantity(command.quantity()), command.limitPrice() == null ? null : new Money(command.limitPrice()), Instant.now());
        orders.put(order.getId(), order);
        try {
            PaperOrderService service = new PaperOrderService(riskGate, broker);
            service.submit(order, command.estimatedNotional(), true, Instant.now(), command.fillPrice(), Instant.now());
        } catch (RuntimeException ex) {
            orders.remove(order.getId());
            throw ex;
        }
        return new OrderView(order.getId(), order.getAccountId(), order.getInstrumentId(), order.getClientOrderId().value(), order.getStatus().name(), order.getSide().name(), order.getOrderType().name(), order.getQuantity().value(), order.getRejectReason());
    }

    public record SubmitOrderCommand(Long id, Long accountId, Long instrumentId, String clientOrderId, String requestId, String idempotencyKey, OrderSide side, OrderType orderType, BigDecimal quantity, BigDecimal limitPrice, Money estimatedNotional, BigDecimal fillPrice) {}
    public record OrderView(Long id, Long accountId, Long instrumentId, String clientOrderId, String status, String side, String orderType, BigDecimal quantity, String rejectReason) {}
}