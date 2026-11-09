package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公司行为视图对象（§13.1）。
 */
@Data
public class CorporateActionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 标的ID */
    private Long instrumentId;

    /** 行为类型 */
    private String actionType;

    /** 行为类型中文描述 */
    private String actionTypeText;

    /** 行为业务编码 */
    private String actionCode;

    /** 生效时间 */
    private LocalDateTime effectiveDate;

    /** 公告时间 */
    private LocalDateTime announceDate;

    /** 事件状态 */
    private String status;

    /** 事件状态中文描述 */
    private String statusText;

    /** 明细（JSON） */
    private String detail;

    /** 来源数据源ID */
    private Long sourceId;

    /** 版本 */
    private String version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}