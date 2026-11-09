package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.persistence.record.PaperExecutionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaperExecutionMapper {
    PaperExecutionRow selectByExecutionId(@Param("executionId") String executionId);
    PaperExecutionRow selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
    int insert(PaperExecutionRow row);
    int countByOrderId(@Param("orderId") Long orderId);
    java.math.BigDecimal sumExecutedQuantity(@Param("orderId") Long orderId);
}

