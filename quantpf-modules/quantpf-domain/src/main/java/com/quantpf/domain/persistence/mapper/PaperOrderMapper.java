package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.persistence.record.PaperOrderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaperOrderMapper {
    PaperOrderRow selectById(@Param("id") Long id);
    java.util.List<PaperOrderRow> selectPage(@Param("offset") int offset, @Param("limit") int limit);
    PaperOrderRow selectByIdForUpdate(@Param("id") Long id);
    PaperOrderRow selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
    PaperOrderRow selectByClientOrderId(@Param("accountId") Long accountId, @Param("clientOrderId") String clientOrderId);
    int insert(PaperOrderRow row);
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("rejectReason") String rejectReason);
    int updateFrozenAmounts(@Param("id") Long id, @Param("frozenCash") java.math.BigDecimal frozenCash, @Param("frozenQuantity") java.math.BigDecimal frozenQuantity);
    int countByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}


