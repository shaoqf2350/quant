package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 股票公司行为实体（《详设_1 §13.1》）。
 * <p>对应表 {@code quantpf_corporate_action}。</p>
 */
@Data
@TableName("quantpf_corporate_action")
public class CorporateAction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 标的ID（关联 quantpf_instrument.id） */
    private Long instrumentId;

    /** 行为类型（CorporateActionType） */
    private String actionType;

    /** 行为业务编码 */
    private String actionCode;

    /** 生效时间 */
    private LocalDateTime effectiveDate;

    /** 公告时间 */
    private LocalDateTime announceDate;

    /** 事件状态（LifecycleEventStatus） */
    private String status;

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