package com.quantpf.finance;

import com.quantpf.domain.entity.PaperExecution;
import com.quantpf.domain.persistence.mapper.PaperExecutionMapper;
import com.quantpf.domain.persistence.record.PaperExecutionRow;
import com.quantpf.domain.repository.PaperExecutionRepository;
import com.quantpf.domain.value.Money;
import com.quantpf.domain.value.Quantity;

import java.time.Instant;
import java.util.Optional;

public class PaperExecutionRepositoryAdapter implements PaperExecutionRepository {
    private final PaperExecutionMapper mapper;
    public PaperExecutionRepositoryAdapter(PaperExecutionMapper mapper) { this.mapper = mapper; }
    @Override public Optional<PaperExecution> findByExecutionId(String executionId) { return toDomain(mapper.selectByExecutionId(executionId)); }
    @Override public Optional<PaperExecution> findByIdempotencyKey(String key) { return toDomain(mapper.selectByIdempotencyKey(key)); }
    @Override public PaperExecution save(PaperExecution execution) {
        PaperExecutionRow row = new PaperExecutionRow();
        row.setOrderId(execution.orderId()); row.setExecutionId(execution.executionId()); row.setExecutedQuantity(execution.quantity().value());
        row.setExecutedPrice(execution.price().value()); row.setFee(execution.fee().value()); row.setExecutedAt(execution.executedAt()); row.setIdempotencyKey(execution.idempotencyKey());
        mapper.insert(row); return execution;
    }
    private static Optional<PaperExecution> toDomain(PaperExecutionRow row) {
        if (row == null) return Optional.empty();
        return Optional.of(new PaperExecution(row.getOrderId(), row.getExecutionId(), new Quantity(row.getExecutedQuantity()), new Money(row.getExecutedPrice()), new Money(row.getFee()), row.getExecutedAt(), row.getIdempotencyKey()));
    }
}
