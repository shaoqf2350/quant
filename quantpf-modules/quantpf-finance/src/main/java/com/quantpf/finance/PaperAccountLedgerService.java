package com.quantpf.finance;
import com.quantpf.domain.entity.*;import com.quantpf.domain.enums.OrderSide;import com.quantpf.domain.value.*;
public class PaperAccountLedgerService{
 public void reserveCash(TradingAccount account,Money amount){account.freezeCash(amount);} public void releaseCash(TradingAccount account,Money amount){account.releaseCash(amount);} public void settleCash(TradingAccount account,Money amount){account.settleFrozenCash(amount);}
 public void reservePosition(Position position,Quantity quantity){position.freeze(quantity);} public void releasePosition(Position position,Quantity quantity){position.release(quantity);} public void settleSell(Position position,Quantity quantity,Money price){position.settleSell(quantity,price);} public void settleBuy(Position position,Quantity quantity,Money price){position.addBought(quantity,price);}
 public Money notional(Money price,Quantity quantity){return price.multiply(quantity);} public boolean requiresPositionFreeze(OrderSide side){return side==OrderSide.SELL;}
}