package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 期货合约生命周期/制度事件实体（《详设_1 §13.2》）。
 * <p>对应表 {@code quantpf_contract_event}。</p>
 */
@Data
@TableName("quantpf_contract_event")
public class ContractEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 合约ID（关联 quantpf_instrument.id） */
    private Long instrumentId;

    /** 事件类型（ContractEventType） */
    private String eventType;

    /** 事件业务编码 */
    private String eventCode;

    /** 生效时间 */
    private LocalDateTime effectiveDate;

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