package com.quantpf.domain.repository;

import com.quantpf.domain.entity.PaperExecution;
import java.util.Optional;

public interface PaperExecutionRepository {
    Optional<PaperExecution> findByExecutionId(String executionId);
    Optional<PaperExecution> findByIdempotencyKey(String idempotencyKey);
    PaperExecution save(PaperExecution execution);
}

