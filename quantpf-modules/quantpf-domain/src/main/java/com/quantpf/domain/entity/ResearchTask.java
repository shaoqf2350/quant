package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 研究任务实体（《详设_1 §10.2》）。
 * <p>对应表 {@code quantpf_research_task}。</p>
 */
@Data
@TableName("quantpf_research_task")
public class ResearchTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 研究任务业务ID */
    private String taskCode;

    /** 研究材料ID集合（逗号分隔） */
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