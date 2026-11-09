package com.quantpf.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 来源组业务对象（查询 + 表单，§6）。
 */
@Data
public class SourceGroupBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（编辑时必传） */
    private Long pkId;

    /** 来源组编码 */
    @NotBlank(message = "来源组编码不能为空")
    @Size(max = 64, message = "来源组编码不能超过{max}个字符")
    private String groupCode;

    /** 来源组名称 */
    @NotBlank(message = "来源组名称不能为空")
    @Size(max = 128, message = "来源组名称不能超过{max}个字符")
    private String groupName;

    /** 主源数据源ID */
    private Long primarySourceId;

    /** 备源数据源ID（逗号分隔） */
    private String backupSourceIds;

    /** 交叉核验规则 */
    private String verifyRule;

    /** 转载/去重识别策略 */
    private String reprintPolicy;

    /** 来源组状态 */
    private String status;
}