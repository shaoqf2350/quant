package com.quantpf.finance;

import com.quantpf.domain.entity.PaperOrder;
import com.quantpf.domain.enums.OrderSide;
import com.quantpf.domain.enums.OrderStatus;
import com.quantpf.domain.enums.OrderType;
import com.quantpf.domain.persistence.mapper.PaperOrderMapper;
import com.quantpf.domain.persistence.record.PaperOrderRow;
import com.quantpf.domain.repository.PaperOrderRepository;
import com.quantpf.domain.value.ClientOrderId;
import com.quantpf.domain.value.Money;
import com.quantpf.domain.value.OrderIdempotencyKey;
import com.quantpf.domain.value.Quantity;
import com.quantpf.domain.value.RequestId;

import java.time.Instant;
import java.util.Optional;

public class PaperOrderRepositoryAdapter implements PaperOrderRepository {
    private final PaperOrderMapper mapper;

    public PaperOrderRepositoryAdapter(PaperOrderMapper mapper) { this.mapper = mapper; }

    @Override public Optional<PaperOrder> findById(Long id) { return toDomain(mapper.selectById(id)); }
    @Override public Optional<PaperOrder> findByIdempotencyKey(String key) { return toDomain(mapper.selectByIdempotencyKey(key)); }
    @Override public Optional<PaperOrder> findByClientOrderId(Long accountId, String clientOrderId) { return toDomain(mapper.selectByClientOrderId(accountId, clientOrderId)); }

    @Override public PaperOrder save(PaperOrder order) {
        PaperOrderRow row = fromDomain(order);
        if (order.getId() == null) {
            mapper.insert(row);
        } else {
            mapper.updateStatus(order.getId(), order.getStatus().name(), order.getRejectReason());
        }
        return order;
    }

    private static Optional<PaperOrder> toDomain(PaperOrderRow row) {
        if (row == null) return Optional.empty();
        PaperOrder order = new PaperOrder(row.getId(), row.getAccountId(), row.getInstrumentId(),
            new ClientOrderId(row.getClientOrderId()), new RequestId(row.getRequestId()),
            new OrderIdempotencyKey(row.getIdempotencyKey()), OrderSide.valueOf(row.getSide()),
            OrderType.valueOf(row.getOrderType()), new Quantity(row.getQuantity()),
            row.getLimitPrice() == null ? null : new Money(row.getLimitPrice()),
            row.getCreatedAt() == null ? Instant.now() : row.getCreatedAt());
        if (row.getStatus() != null && !OrderStatus.NEW.name().equals(row.getStatus())) {
            order.setStatus(OrderStatus.valueOf(row.getStatus()));
        }
        if (row.getRejectReason() != null) order.reject(row.getRejectReason());
        return Optional.of(order);
    }

    private static PaperOrderRow fromDomain(PaperOrder order) {
        PaperOrderRow row = new PaperOrderRow();
        row.setId(order.getId()); row.setAccountId(order.getAccountId()); row.setInstrumentId(order.getInstrumentId());
        row.setClientOrderId(order.getClientOrderId().value()); row.setRequestId(order.getRequestId().value());
        row.setIdempotencyKey(order.getIdempotencyKey().value()); row.setSide(order.getSide().name());
        row.setOrderType(order.getOrderType().name()); row.setQuantity(order.getQuantity().value());
        row.setLimitPrice(order.getLimitPrice() == null ? null : order.getLimitPrice().value());
        row.setStatus(order.getStatus().name()); row.setRejectReason(order.getRejectReason());
        row.setFrozenCash(java.math.BigDecimal.ZERO); row.setFrozenQuantity(java.math.BigDecimal.ZERO);
        return row;
    }
}
