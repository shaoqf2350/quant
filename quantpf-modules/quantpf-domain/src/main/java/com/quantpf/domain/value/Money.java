package com.quantpf.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal value) {
    public Money {
        Objects.requireNonNull(value, "value");
        value = value.setScale(8, RoundingMode.HALF_UP);
    }
    public static Money zero() { return new Money(BigDecimal.ZERO); }
    public Money add(Money other) { return new Money(value.add(other.value)); }
    public Money subtract(Money other) { return new Money(value.subtract(other.value)); }
    public Money multiply(Quantity quantity) { return new Money(value.multiply(quantity.value())); }
    public boolean isNegative() { return value.signum() < 0; }
    public boolean isPositive() { return value.signum() > 0; }
    public int compareTo(Money other) { return value.compareTo(other.value); }
}