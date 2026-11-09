package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 研究任务视图对象（§10.2）。
 */
@Data
public class ResearchTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 研究任务业务ID */
    private String taskCode;

    /** 研究材料ID集合 */
    private String materialIds;

    /** 标的/合约/现货引用 */
    private String subjectRefs;

    /** 任务类型 */
    private String taskType;

    /** 点时快照ID */
    private Long snapshotId;

    /** Dify Workflow 版本 */
    private String workflowVersion;

    /** Prompt 版本 */
    private String promptVersion;

    /** 任务状态 */
    private String status;

    /** 任务状态中文描述 */
    private String statusText;

    /** 幂等键 */
    private String idempotencyKey;

    /** Dify/Agent 运行ID */
    private String runId;

    /** 异常码 */
    private String errorCode;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}