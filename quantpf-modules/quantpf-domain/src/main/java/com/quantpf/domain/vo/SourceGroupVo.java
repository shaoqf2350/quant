package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 来源组视图对象（§6，含状态中文描述）。
 */
@Data
public class SourceGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 来源组编码 */
    private String groupCode;

    /** 来源组名称 */
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

    /** 来源组状态中文描述 */
    private String statusText;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}