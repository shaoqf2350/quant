package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.persistence.record.PositionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PositionMapper {
    PositionRow selectByAccountAndInstrument(@Param("accountId") Long accountId, @Param("instrumentId") Long instrumentId);
    PositionRow selectByAccountAndInstrumentForUpdate(@Param("accountId") Long accountId, @Param("instrumentId") Long instrumentId);
    int insert(PositionRow row);
    int updateState(PositionRow row);
}
