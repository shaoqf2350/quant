package com.quantpf.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 市场状态记录表单（§14.4）。
 */
@Data
public class MarketStateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标的ID（NULL=市场级） */
    private Long instrumentId;

    /** 日历ID */
    private Long calendarId;

    /** 市场本地日期 */
    @NotNull(message = "市场本地日期不能为空")
    private LocalDate marketLocalDate;

    /** 市场状态 */
    @NotBlank(message = "市场状态不能为空")
    private String stateCode;

    /** 状态原因 */
    private String stateReason;
}