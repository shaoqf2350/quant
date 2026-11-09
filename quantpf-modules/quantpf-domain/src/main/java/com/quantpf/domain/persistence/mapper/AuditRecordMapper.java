package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.persistence.record.AuditRecordRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AuditRecordMapper {
    List<AuditRecordRow> selectPage(@Param("offset") long offset,
                                    @Param("limit") int limit,
                                    @Param("requestId") String requestId,
                                    @Param("aggregateType") String aggregateType,
                                    @Param("aggregateId") String aggregateId);
    int insert(AuditRecordRow row);
}