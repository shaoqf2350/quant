package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.persistence.record.RiskSnapshotRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RiskSnapshotMapper {
    int insert(RiskSnapshotRow row);
    RiskSnapshotRow selectLatestByAccountId(@Param("accountId") Long accountId);
}
