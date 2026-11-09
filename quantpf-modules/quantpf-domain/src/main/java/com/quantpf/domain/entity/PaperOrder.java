package com.quantpf.domain.entity;

import com.quantpf.domain.enums.OrderSide; import com.quantpf.domain.enums.OrderStatus; import com.quantpf.domain.enums.OrderType;
import com.quantpf.domain.value.*; import java.time.Instant; import java.util.Objects;

public class PaperOrder {
    private final Long id,accountId,instrumentId; private final ClientOrderId clientOrderId; private final RequestId requestId; private final OrderIdempotencyKey idempotencyKey;
    private final OrderSide side; private final OrderType orderType; private final Quantity quantity; private final Money limitPrice; private OrderStatus status=OrderStatus.NEW; private String rejectReason; private final Instant createdAt;
    public PaperOrder(Long id,Long accountId,Long instrumentId,ClientOrderId clientOrderId,RequestId requestId,OrderIdempotencyKey idempotencyKey,OrderSide side,OrderType orderType,Quantity quantity,Money limitPrice,Instant createdAt){
        this.id=id;this.accountId=accountId;this.instrumentId=instrumentId;this.clientOrderId=Objects.requireNonNull(clientOrderId);this.requestId=Objects.requireNonNull(requestId);this.idempotencyKey=Objects.requireNonNull(idempotencyKey);this.side=Objects.requireNonNull(side);this.orderType=Objects.requireNonNull(orderType);this.quantity=Objects.requireNonNull(quantity);this.limitPrice=limitPrice;this.createdAt=Objects.requireNonNull(createdAt);
        if(!quantity.isPositive())throw new IllegalArgumentException("quantity must be positive"); if(orderType==OrderType.LIMIT&&(limitPrice==null||!limitPrice.isPositive()))throw new IllegalArgumentException("limit price required");
    }
    public Long getId(){return id;} public Long getAccountId(){return accountId;} public Long getInstrumentId(){return instrumentId;} public ClientOrderId getClientOrderId(){return clientOrderId;} public RequestId getRequestId(){return requestId;} public OrderIdempotencyKey getIdempotencyKey(){return idempotencyKey;} public OrderSide getSide(){return side;} public OrderType getOrderType(){return orderType;} public Quantity getQuantity(){return quantity;} public Money getLimitPrice(){return limitPrice;} public OrderStatus getStatus(){return status;} public String getRejectReason(){return rejectReason;} public Instant getCreatedAt(){return createdAt;}
    public void setStatus(OrderStatus status){this.status=Objects.requireNonNull(status);} public void reject(String reason){status=OrderStatus.REJECTED;rejectReason=reason;}
}