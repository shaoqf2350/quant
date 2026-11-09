package com.quantpf.finance;

import com.quantpf.domain.persistence.mapper.JobExecutionAuditMapper;
import com.quantpf.domain.persistence.record.JobExecutionAuditRow;
import com.quantpf.domain.repository.JobExecutionAuditRepository;

import java.util.List;
import java.util.Objects;

public class JobExecutionAuditRepositoryAdapter implements JobExecutionAuditRepository {
    private final JobExecutionAuditMapper mapper;

    public JobExecutionAuditRepositoryAdapter(JobExecutionAuditMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public List<JobExecutionAuditRow> page(long offset, int limit, String jobName, String executionId, String status) {
        return mapper.selectPage(offset, limit, jobName, executionId, status);
    }
}