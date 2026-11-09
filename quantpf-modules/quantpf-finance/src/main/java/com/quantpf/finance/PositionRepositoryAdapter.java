package com.quantpf.finance;

import com.quantpf.domain.entity.Position;
import com.quantpf.domain.persistence.mapper.PositionMapper;
import com.quantpf.domain.persistence.record.PositionRow;
import com.quantpf.domain.repository.PositionRepository;
import com.quantpf.domain.value.Money;
import com.quantpf.domain.value.Quantity;

import java.util.Optional;

public class PositionRepositoryAdapter implements PositionRepository {
    private final PositionMapper mapper;
    public PositionRepositoryAdapter(PositionMapper mapper) { this.mapper = mapper; }
    @Override public Optional<Position> findByAccountAndInstrument(Long accountId, Long instrumentId) { return toDomain(mapper.selectByAccountAndInstrument(accountId, instrumentId)); }
    @Override public Position save(Position position) {
        PositionRow row = fromDomain(position);
        PositionRow existing = mapper.selectByAccountAndInstrument(position.getAccountId(), position.getInstrumentId());
        if (existing == null) mapper.insert(row); else { row.setId(existing.getId()); mapper.updateState(row); }
        return position;
    }
    private static Optional<Position> toDomain(PositionRow row) {
        if (row == null) return Optional.empty();
        Position position = new Position(row.getAccountId(), row.getInstrumentId());
        position.restoreState(new Quantity(row.getQuantity()), new Quantity(row.getAvailableQuantity()), new Quantity(row.getFrozenQuantity()), new Money(row.getAverageCost()), new Money(row.getRealizedPnl()));
        return Optional.of(position);
    }
    private static PositionRow fromDomain(Position position) {
        PositionRow row = new PositionRow(); row.setAccountId(position.getAccountId()); row.setInstrumentId(position.getInstrumentId());
        row.setQuantity(position.getQuantity().value()); row.setAvailableQuantity(position.getAvailableQuantity().value()); row.setFrozenQuantity(position.getFrozenQuantity().value());
        row.setAverageCost(position.getAverageCost().value()); row.setRealizedPnl(position.getRealizedPnl().value()); return row;
    }
}
