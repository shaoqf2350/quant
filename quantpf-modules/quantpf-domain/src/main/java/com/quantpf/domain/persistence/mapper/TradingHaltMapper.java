package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.persistence.record.TradingHaltRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TradingHaltMapper {
    int countActive(@Param("scope") String scope, @Param("targetId") String targetId);
    TradingHaltRow selectActive(@Param("scope") String scope, @Param("targetId") String targetId);
    int insert(TradingHaltRow row);
    int resume(@Param("id") Long id, @Param("resumedAt") java.time.Instant resumedAt, @Param("resumeReason") String resumeReason);
}
