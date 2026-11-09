package com.quantpf.domain.entity;

import com.quantpf.domain.exception.QuantPfDomainException;
import com.quantpf.domain.value.Money;
import com.quantpf.domain.value.Quantity;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Position {
    private final Long accountId; private final Long instrumentId;
    private Quantity quantity=Quantity.zero(); private Quantity availableQuantity=Quantity.zero(); private Quantity frozenQuantity=Quantity.zero();
    private Money averageCost=Money.zero(); private Money realizedPnl=Money.zero();
    public Position(Long accountId,Long instrumentId){this.accountId=accountId;this.instrumentId=instrumentId;}
    public Long getAccountId(){return accountId;} public Long getInstrumentId(){return instrumentId;} public Quantity getQuantity(){return quantity;}
    public Quantity getAvailableQuantity(){return availableQuantity;} public Quantity getFrozenQuantity(){return frozenQuantity;} public Money getAverageCost(){return averageCost;} public Money getRealizedPnl(){return realizedPnl;} public void restoreState(Quantity quantity, Quantity availableQuantity, Quantity frozenQuantity, Money averageCost, Money realizedPnl){this.quantity=quantity;this.availableQuantity=availableQuantity;this.frozenQuantity=frozenQuantity;this.averageCost=averageCost;this.realizedPnl=realizedPnl;}
    public void addBought(Quantity amount,Money price){if(!amount.isPositive())throw new IllegalArgumentException("quantity must be positive"); BigDecimal old=averageCost.value().multiply(quantity.value()); BigDecimal total=old.add(price.value().multiply(amount.value())); quantity=quantity.add(amount);availableQuantity=availableQuantity.add(amount);averageCost=new Money(total.divide(quantity.value(),8,RoundingMode.HALF_UP));}
    public void freeze(Quantity amount){if(availableQuantity.compareTo(amount)<0)throw new QuantPfDomainException("QPF-POSITION-001","Insufficient position");availableQuantity=availableQuantity.subtract(amount);frozenQuantity=frozenQuantity.add(amount);}
    public void release(Quantity amount){if(frozenQuantity.compareTo(amount)<0)throw new QuantPfDomainException("QPF-POSITION-001","Frozen position is insufficient");frozenQuantity=frozenQuantity.subtract(amount);availableQuantity=availableQuantity.add(amount);}
    public void settleSell(Quantity amount,Money price){if(frozenQuantity.compareTo(amount)<0||quantity.compareTo(amount)<0)throw new QuantPfDomainException("QPF-POSITION-001","Frozen position is insufficient");frozenQuantity=frozenQuantity.subtract(amount);quantity=quantity.subtract(amount);realizedPnl=realizedPnl.add(price.subtract(averageCost).multiply(amount));}
}
