package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.persistence.record.JobExecutionAuditRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JobExecutionAuditMapper {
    List<JobExecutionAuditRow> selectPage(@Param("offset") long offset,
                                          @Param("limit") int limit,
                                          @Param("jobName") String jobName,
                                          @Param("executionId") String executionId,
                                          @Param("status") String status);
}