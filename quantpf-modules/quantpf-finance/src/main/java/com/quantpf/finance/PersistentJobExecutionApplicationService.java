package com.quantpf.finance;

import com.quantpf.domain.persistence.record.JobExecutionAuditRow;
import com.quantpf.domain.repository.JobExecutionAuditRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Database-backed Paper job execution audit read boundary. No scheduler is started or connected here. */
public final class PersistentJobExecutionApplicationService {
    private final JobExecutionAuditRepository repository;

    public PersistentJobExecutionApplicationService(JobExecutionAuditRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<JobExecutionView> page(int pageNum, int pageSize) {
        return page(pageNum, pageSize, null, null, null);
    }

    public List<JobExecutionView> page(int pageNum, int pageSize, String jobName, String executionId, String status) {
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        long safePageNum = Math.max((long) pageNum, 1L);
        long offset = (safePageNum - 1L) * safePageSize;
        List<JobExecutionAuditRow> rows = Objects.requireNonNull(
            repository.page(offset, safePageSize, normalize(jobName), normalize(executionId), normalize(status)),
            "Job execution repository returned null");
        return rows.stream().map(PersistentJobExecutionApplicationService::toView).toList();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static JobExecutionView toView(JobExecutionAuditRow row) {
        return new JobExecutionView(row.getId(), row.getJobName(), row.getExecutionId(), row.getSchedulerType(),
            row.getStatus(), row.getRequestId(), row.getStartedAt(), row.getFinishedAt(), row.getErrorCode(),
            row.getErrorMessage(), row.getCreatedAt());
    }

    public record JobExecutionView(Long id, String jobName, String executionId, String schedulerType,
                                   String status, String requestId, Instant startedAt, Instant finishedAt,
                                   String errorCode, String errorMessage, Instant createdAt) {}
}