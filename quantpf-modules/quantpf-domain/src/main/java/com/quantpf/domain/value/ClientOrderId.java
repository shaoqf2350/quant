package com.quantpf.domain.value;
import java.util.Objects;
public record ClientOrderId(String value) {
    public ClientOrderId { Objects.requireNonNull(value, "value"); if (value.isBlank() || value.length() > 128) throw new IllegalArgumentException("invalid client order id"); }
}