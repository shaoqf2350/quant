package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.persistence.record.TradingAccountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TradingAccountMapper {
    TradingAccountRow selectById(@Param("id") Long id);
    java.util.List<TradingAccountRow> selectPage(@Param("offset") int offset, @Param("limit") int limit);
    TradingAccountRow selectByIdForUpdate(@Param("id") Long id);
    int insert(TradingAccountRow row);
    int updateBalances(@Param("id") Long id, @Param("availableCash") java.math.BigDecimal availableCash, @Param("frozenCash") java.math.BigDecimal frozenCash, @Param("status") String status);
    int updateBalancesIfExpected(@Param("id") Long id, @Param("expectedAvailableCash") java.math.BigDecimal expectedAvailableCash, @Param("expectedFrozenCash") java.math.BigDecimal expectedFrozenCash, @Param("availableCash") java.math.BigDecimal availableCash, @Param("frozenCash") java.math.BigDecimal frozenCash, @Param("status") String status);
}


