package com.quantpf.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 研究任务业务对象（创建表单，§10.2）。
 */
@Data
public class ResearchTaskBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 研究材料ID集合（逗号分隔） */
    private String materialIds;

    /** 标的/合约/现货引用 */
    private String subjectRefs;

    /** 任务类型 */
    @NotBlank(message = "任务类型不能为空")
    @Size(max = 32, message = "任务类型不能超过{max}个字符")
    private String taskType;

    /** 点时快照ID */
    private Long snapshotId;

    /** Dify Workflow 版本 */
    private String workflowVersion;

    /** Prompt 版本 */
    private String promptVersion;

    /** 幂等键（不传则自动生成） */
    private String idempotencyKey;
}