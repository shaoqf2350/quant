package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 期货合约事件视图对象（§13.2）。
 */
@Data
public class ContractEventVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 合约ID */
    private Long instrumentId;

    /** 事件类型 */
    private String eventType;

    /** 事件类型中文描述 */
    private String eventTypeText;

    /** 事件业务编码 */
    private String eventCode;

    /** 生效时间 */
    private LocalDateTime effectiveDate;

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