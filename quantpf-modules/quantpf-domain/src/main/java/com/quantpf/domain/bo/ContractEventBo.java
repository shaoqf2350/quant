package com.quantpf.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 期货合约事件业务对象（创建/编辑/查询，§13.2）。
 */
@Data
public class ContractEventBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（编辑时必传） */
    private Long pkId;

    /** 合约ID */
    @NotNull(message = "合约ID不能为空")
    private Long instrumentId;

    /** 事件类型 */
    @NotBlank(message = "事件类型不能为空")
    @Size(max = 32, message = "事件类型不能超过{max}个字符")
    private String eventType;

    /** 事件业务编码（不传则自动生成） */
    private String eventCode;

    /** 生效时间 */
    private LocalDateTime effectiveDate;

    /** 事件状态 */
    private String status;

    /** 明细（JSON） */
    private String detail;

    /** 来源数据源ID */
    private Long sourceId;
}