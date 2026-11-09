package com.quantpf.domain.value;
import java.util.Objects;
public record OrderIdempotencyKey(String value) {
    public OrderIdempotencyKey { Objects.requireNonNull(value, "value"); if (value.isBlank() || value.length() > 128) throw new IllegalArgumentException("invalid idempotency key"); }
}