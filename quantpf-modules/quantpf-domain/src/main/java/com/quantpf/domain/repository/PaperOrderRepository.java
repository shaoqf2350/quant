package com.quantpf.domain.repository;

import com.quantpf.domain.entity.PaperOrder;
import java.util.Optional;

public interface PaperOrderRepository {
    Optional<PaperOrder> findById(Long id);
    Optional<PaperOrder> findByIdempotencyKey(String key);
    Optional<PaperOrder> findByClientOrderId(Long accountId, String clientOrderId);
    PaperOrder save(PaperOrder order);
}
