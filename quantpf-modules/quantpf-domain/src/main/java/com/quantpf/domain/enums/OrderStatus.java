package com.quantpf.domain.enums;
public enum OrderStatus {
    NEW, ACCEPTED, PARTIALLY_FILLED, FILLED, CANCEL_REQUESTED, CANCELED, REJECTED;
    public boolean isTerminal() { return this == FILLED || this == CANCELED || this == REJECTED; }
}