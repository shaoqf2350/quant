package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.persistence.record.CashLedgerRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CashLedgerMapper {
    java.util.List<CashLedgerRow> selectByAccountId(@Param("accountId") Long accountId);
    CashLedgerRow selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
    int insert(CashLedgerRow row);
    int countByReferenceId(@Param("referenceId") String referenceId);
}


