package com.quantpf.finance;

import com.quantpf.domain.persistence.mapper.CashLedgerMapper;
import com.quantpf.domain.persistence.mapper.PaperExecutionMapper;
import com.quantpf.domain.persistence.mapper.PaperOrderMapper;
import com.quantpf.domain.persistence.mapper.PositionMapper;
import com.quantpf.domain.persistence.mapper.TradingAccountMapper;
import com.quantpf.domain.persistence.record.CashLedgerRow;
import com.quantpf.domain.persistence.record.PaperExecutionRow;
import com.quantpf.domain.persistence.record.PaperOrderRow;
import com.quantpf.domain.persistence.record.PositionRow;
import com.quantpf.domain.persistence.record.TradingAccountRow;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/** One explicit MyBatis transaction per Paper command. */
public final class PaperPersistenceTransactionService {
    private final SqlSessionFactory sessionFactory;

    public PaperPersistenceTransactionService(SqlSessionFactory sessionFactory) { this.sessionFactory = sessionFactory; }

    public SubmissionResult submit(PaperOrderRow requested) {
        require(requested.getAccountId(), "accountId"); require(requested.getClientOrderId(), "clientOrderId"); require(requested.getIdempotencyKey(), "idempotencyKey");
        try (SqlSession session = sessionFactory.openSession(false)) {
            TradingAccountMapper accounts = session.getMapper(TradingAccountMapper.class);
            PaperOrderMapper orders = session.getMapper(PaperOrderMapper.class);
            PositionMapper positions = session.getMapper(PositionMapper.class);
            TradingAccountRow account = accounts.selectByIdForUpdate(requested.getAccountId());
            if (account == null) throw new IllegalStateException("QPF-ACCOUNT-001: account not found");
            PaperOrderRow existing = orders.selectByIdempotencyKey(requested.getIdempotencyKey());
            if (existing == null) existing = orders.selectByClientOrderId(requested.getAccountId(), requested.getClientOrderId());
            if (existing != null) { session.rollback(); return new SubmissionResult(existing.getId(), true); }

            BigDecimal frozenCash = nvl(requested.getFrozenCash());
            BigDecimal frozenQuantity = nvl(requested.getFrozenQuantity());
            if (frozenCash.signum() > 0) {
                if (nvl(account.getAvailableCash()).compareTo(frozenCash) < 0) throw new IllegalStateException("QPF-CASH-001: insufficient available cash");
                account.setAvailableCash(nvl(account.getAvailableCash()).subtract(frozenCash));
                account.setFrozenCash(nvl(account.getFrozenCash()).add(frozenCash));
            }
            if (frozenQuantity.signum() > 0) {
                PositionRow position = positions.selectByAccountAndInstrumentForUpdate(requested.getAccountId(), requested.getInstrumentId());
                if (position == null || nvl(position.getAvailableQuantity()).compareTo(frozenQuantity) < 0) throw new IllegalStateException("QPF-POSITION-001: insufficient available position");
                position.setAvailableQuantity(nvl(position.getAvailableQuantity()).subtract(frozenQuantity));
                position.setFrozenQuantity(nvl(position.getFrozenQuantity()).add(frozenQuantity));
                positions.updateState(position);
            }
            accounts.updateBalances(account.getId(), account.getAvailableCash(), account.getFrozenCash(), account.getStatus());
            requested.setStatus(requested.getStatus() == null ? "NEW" : requested.getStatus());
            requested.setFrozenCash(frozenCash); requested.setFrozenQuantity(frozenQuantity); orders.insert(requested);
            session.commit(); return new SubmissionResult(requested.getId(), false);
        } catch (RuntimeException failure) {
            SubmissionResult duplicate = findExistingSubmission(requested);
            if (duplicate != null) return duplicate;
            throw failure;
        }
    }

    public SettlementResult settle(SettlementCommand command) {
        require(command.orderId(), "orderId"); require(command.executionId(), "executionId"); require(command.idempotencyKey(), "idempotencyKey");
        if (command.quantity() == null || command.quantity().signum() <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (command.price() == null || command.price().signum() <= 0) throw new IllegalArgumentException("price must be positive");
        BigDecimal fee = nvl(command.fee());
        BigDecimal gross = command.quantity().multiply(command.price()).add(fee).setScale(8, RoundingMode.HALF_UP);

        try (SqlSession session = sessionFactory.openSession(false)) {
            PaperOrderMapper orders = session.getMapper(PaperOrderMapper.class);
            PaperExecutionMapper executions = session.getMapper(PaperExecutionMapper.class);
            CashLedgerMapper ledgers = session.getMapper(CashLedgerMapper.class);
            TradingAccountMapper accounts = session.getMapper(TradingAccountMapper.class);
            PositionMapper positions = session.getMapper(PositionMapper.class);

            PaperExecutionRow duplicate = executions.selectByExecutionId(command.executionId());
            if (duplicate == null) duplicate = executions.selectByIdempotencyKey(command.idempotencyKey());
            if (duplicate != null) { session.rollback(); return new SettlementResult(duplicate.getId(), true); }
            CashLedgerRow existingLedger = ledgers.selectByIdempotencyKey(command.ledgerIdempotencyKey());
            if (existingLedger != null) { session.rollback(); return new SettlementResult(null, true); }

            PaperOrderRow order = orders.selectByIdForUpdate(command.orderId());
            if (order == null) throw new IllegalStateException("QPF-ORDER-001: order not found");
            if ("CANCELED".equals(order.getStatus()) || "REJECTED".equals(order.getStatus()) || "FILLED".equals(order.getStatus())) throw new IllegalStateException("QPF-ORDER-001: terminal order cannot be settled");
            BigDecimal filledBefore = nvl(executions.sumExecutedQuantity(order.getId()));
            BigDecimal remainingBefore = nvl(order.getQuantity()).subtract(filledBefore);
            if (command.quantity().compareTo(remainingBefore) > 0) throw new IllegalStateException("QPF-ORDER-002: execution exceeds remaining quantity");

            TradingAccountRow account = accounts.selectByIdForUpdate(order.getAccountId());
            if (account == null) throw new IllegalStateException("QPF-ACCOUNT-001: account not found");
            PaperExecutionRow execution = new PaperExecutionRow();
            execution.setOrderId(order.getId()); execution.setExecutionId(command.executionId()); execution.setExecutedQuantity(command.quantity()); execution.setExecutedPrice(command.price()); execution.setFee(fee); execution.setExecutedAt(command.executedAt() == null ? Instant.now() : command.executedAt()); execution.setIdempotencyKey(command.idempotencyKey());
            executions.insert(execution);

            BigDecimal filledAfter = filledBefore.add(command.quantity());
            boolean finalFill = filledAfter.compareTo(nvl(order.getQuantity())) >= 0;
            boolean buy = "BUY".equals(order.getSide());
            BigDecimal availableCash = nvl(account.getAvailableCash());
            BigDecimal frozenCash = nvl(account.getFrozenCash());
            BigDecimal nextOrderFrozenCash = nvl(order.getFrozenCash()).subtract(buy ? gross : BigDecimal.ZERO).max(BigDecimal.ZERO).setScale(8);
            BigDecimal nextOrderFrozenQuantity = nvl(order.getFrozenQuantity()).subtract(!buy ? command.quantity() : BigDecimal.ZERO).max(BigDecimal.ZERO).setScale(8);

            PositionRow position = positions.selectByAccountAndInstrumentForUpdate(order.getAccountId(), order.getInstrumentId());
            if (buy) {
                if (frozenCash.compareTo(gross) < 0) throw new IllegalStateException("QPF-CASH-001: frozen cash is insufficient");
                frozenCash = frozenCash.subtract(gross);
                if (position == null) {
                    position = new PositionRow(); position.setAccountId(order.getAccountId()); position.setInstrumentId(order.getInstrumentId()); position.setQuantity(command.quantity()); position.setAvailableQuantity(command.quantity()); position.setFrozenQuantity(BigDecimal.ZERO); position.setAverageCost(command.price()); position.setRealizedPnl(BigDecimal.ZERO); positions.insert(position);
                } else {
                    BigDecimal oldValue = nvl(position.getAverageCost()).multiply(nvl(position.getQuantity()));
                    BigDecimal totalQty = nvl(position.getQuantity()).add(command.quantity());
                    position.setQuantity(totalQty); position.setAvailableQuantity(nvl(position.getAvailableQuantity()).add(command.quantity())); position.setAverageCost(oldValue.add(command.price().multiply(command.quantity())).divide(totalQty, 8, RoundingMode.HALF_UP)); positions.updateState(position);
                }
                if (finalFill && nextOrderFrozenCash.signum() > 0) { availableCash = availableCash.add(nextOrderFrozenCash); frozenCash = frozenCash.subtract(nextOrderFrozenCash); nextOrderFrozenCash = BigDecimal.ZERO.setScale(8); }
            } else {
                if (position == null || nvl(position.getFrozenQuantity()).compareTo(command.quantity()) < 0) throw new IllegalStateException("QPF-POSITION-001: frozen position is insufficient");
                position.setFrozenQuantity(nvl(position.getFrozenQuantity()).subtract(command.quantity())); position.setQuantity(nvl(position.getQuantity()).subtract(command.quantity())); position.setRealizedPnl(nvl(position.getRealizedPnl()).add(command.price().subtract(nvl(position.getAverageCost())).multiply(command.quantity())));
                if (finalFill && nextOrderFrozenQuantity.signum() > 0) { position.setFrozenQuantity(nvl(position.getFrozenQuantity()).subtract(nextOrderFrozenQuantity)); position.setAvailableQuantity(nvl(position.getAvailableQuantity()).add(nextOrderFrozenQuantity)); nextOrderFrozenQuantity = BigDecimal.ZERO.setScale(8); }
                positions.updateState(position); availableCash = availableCash.add(gross);
            }
            account.setAvailableCash(availableCash); account.setFrozenCash(frozenCash); accounts.updateBalances(account.getId(), availableCash, frozenCash, account.getStatus());
            CashLedgerRow ledger = new CashLedgerRow(); ledger.setAccountId(order.getAccountId()); ledger.setLedgerType(buy ? "PAPER_BUY_SETTLEMENT" : "PAPER_SELL_SETTLEMENT"); ledger.setAmount(buy ? gross.negate() : gross); ledger.setBalanceAfter(availableCash); ledger.setReferenceType("PAPER_EXECUTION"); ledger.setReferenceId(command.executionId()); ledger.setIdempotencyKey(command.ledgerIdempotencyKey()); ledger.setOccurredAt(execution.getExecutedAt()); ledgers.insert(ledger);
            if (command.failAfterLedger()) throw new IllegalStateException("QPF-TEST-FAIL-AFTER-LEDGER");
            String nextStatus = finalFill ? "FILLED" : "PARTIALLY_FILLED";
            orders.updateFrozenAmounts(order.getId(), finalFill ? BigDecimal.ZERO : nextOrderFrozenCash, finalFill ? BigDecimal.ZERO : nextOrderFrozenQuantity); orders.updateStatus(order.getId(), nextStatus, null);
            session.commit(); return new SettlementResult(execution.getId(), false);
        } catch (RuntimeException failure) {
            SettlementResult duplicate = findExistingSettlement(command); if (duplicate != null) return duplicate; throw failure;
        }
    }

    public CancelResult cancel(Long orderId, String reason) {
        require(orderId, "orderId");
        try (SqlSession session = sessionFactory.openSession(false)) {
            PaperOrderMapper orders = session.getMapper(PaperOrderMapper.class); TradingAccountMapper accounts = session.getMapper(TradingAccountMapper.class); PositionMapper positions = session.getMapper(PositionMapper.class);
            PaperOrderRow order = orders.selectByIdForUpdate(orderId); if (order == null) throw new IllegalStateException("QPF-ORDER-001: order not found");
            if ("CANCELED".equals(order.getStatus())) { session.rollback(); return new CancelResult(orderId, true); }
            if ("FILLED".equals(order.getStatus()) || "REJECTED".equals(order.getStatus())) throw new IllegalStateException("QPF-ORDER-003: terminal order cannot be canceled");
            TradingAccountRow account = accounts.selectByIdForUpdate(order.getAccountId()); if (account == null) throw new IllegalStateException("QPF-ACCOUNT-001: account not found");
            BigDecimal frozenCash = nvl(order.getFrozenCash()); if (frozenCash.signum() > 0) { account.setFrozenCash(nvl(account.getFrozenCash()).subtract(frozenCash)); account.setAvailableCash(nvl(account.getAvailableCash()).add(frozenCash)); }
            BigDecimal frozenQty = nvl(order.getFrozenQuantity()); if (frozenQty.signum() > 0) { PositionRow position = positions.selectByAccountAndInstrumentForUpdate(order.getAccountId(), order.getInstrumentId()); if (position == null || nvl(position.getFrozenQuantity()).compareTo(frozenQty)<0) throw new IllegalStateException("QPF-POSITION-001: frozen position is insufficient"); position.setFrozenQuantity(nvl(position.getFrozenQuantity()).subtract(frozenQty)); position.setAvailableQuantity(nvl(position.getAvailableQuantity()).add(frozenQty)); positions.updateState(position); }
            accounts.updateBalances(account.getId(), account.getAvailableCash(), account.getFrozenCash(), account.getStatus()); orders.updateFrozenAmounts(orderId, BigDecimal.ZERO, BigDecimal.ZERO); orders.updateStatus(orderId, "CANCELED", reason); session.commit(); return new CancelResult(orderId, false);
        }
    }

    private SubmissionResult findExistingSubmission(PaperOrderRow r) { try (SqlSession s=sessionFactory.openSession(true)) { PaperOrderMapper m=s.getMapper(PaperOrderMapper.class); PaperOrderRow row=m.selectByIdempotencyKey(r.getIdempotencyKey()); if(row==null)row=m.selectByClientOrderId(r.getAccountId(),r.getClientOrderId()); return row==null?null:new SubmissionResult(row.getId(),true); } }
    private SettlementResult findExistingSettlement(SettlementCommand c) { try (SqlSession s=sessionFactory.openSession(true)) { PaperExecutionMapper m=s.getMapper(PaperExecutionMapper.class); PaperExecutionRow row=m.selectByExecutionId(c.executionId()); if(row==null)row=m.selectByIdempotencyKey(c.idempotencyKey()); return row==null?null:new SettlementResult(row.getId(),true); } }
    private static BigDecimal nvl(BigDecimal v){return v==null?BigDecimal.ZERO.setScale(8):v.setScale(8,RoundingMode.HALF_UP);}
    private static void require(Object v,String n){if(v==null||(v instanceof String s&&s.isBlank()))throw new IllegalArgumentException(n+" is required");}
    public record SubmissionResult(Long orderId, boolean duplicate) {}
    public record SettlementResult(Long executionId, boolean duplicate) {}
    public record CancelResult(Long orderId, boolean duplicate) {}
    public record SettlementCommand(Long orderId,String executionId,String idempotencyKey,BigDecimal quantity,BigDecimal price,BigDecimal fee,Instant executedAt,String ledgerIdempotencyKey,boolean failAfterLedger){public SettlementCommand{if(ledgerIdempotencyKey==null||ledgerIdempotencyKey.isBlank())ledgerIdempotencyKey="ledger:"+executionId;}}
}
