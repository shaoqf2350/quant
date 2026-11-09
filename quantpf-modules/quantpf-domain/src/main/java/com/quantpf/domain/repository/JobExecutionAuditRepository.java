package com.quantpf.domain.repository;

import com.quantpf.domain.persistence.record.JobExecutionAuditRow;

import java.util.List;

public interface JobExecutionAuditRepository {
    List<JobExecutionAuditRow> page(long offset, int limit, String jobName, String executionId, String status);
}