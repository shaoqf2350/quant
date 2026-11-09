package com.quantpf.domain.value;
import java.util.Objects;
public record RequestId(String value) {
    public RequestId { Objects.requireNonNull(value, "value"); if (value.isBlank() || value.length() > 128) throw new IllegalArgumentException("invalid request id"); }
}