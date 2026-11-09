package com.quantpf.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Quantity(BigDecimal value) {
    public Quantity {
        Objects.requireNonNull(value, "value");
        value = value.setScale(8, RoundingMode.HALF_UP);
        if (value.signum() < 0) throw new IllegalArgumentException("quantity must not be negative");
    }
    public static Quantity zero() { return new Quantity(BigDecimal.ZERO); }
    public Quantity add(Quantity other) { return new Quantity(value.add(other.value)); }
    public Quantity subtract(Quantity other) { return new Quantity(value.subtract(other.value)); }
    public boolean isPositive() { return value.signum() > 0; }
    public int compareTo(Quantity other) { return value.compareTo(other.value); }
}