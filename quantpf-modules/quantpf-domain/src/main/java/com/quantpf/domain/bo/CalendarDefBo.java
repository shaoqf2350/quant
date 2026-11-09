package com.quantpf.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 交易日历定义业务对象（查询 + 表单，§12.3.1）。
 */
@Data
public class CalendarDefBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（编辑时必传） */
    private Long pkId;

    /** 日历编码 */
    @NotBlank(message = "日历编码不能为空")
    @Size(max = 64, message = "日历编码不能超过{max}个字符")
    private String calendarCode;

    /** 日历名称 */
    private String calendarName;

    /** 市场类型（STOCK/FUTURE/FX/SPOT/INDEX） */
    private String marketType;

    /** 地区（CN/HK/US/GLOBAL） */
    private String regionCode;

    /** 交易场所 */
    private String venueCode;

    /** 市场本地时区 */
    private String timezone;

    /** 币种 */
    private String currency;

    /** 级别 */
    private String calendarLevel;

    /** 来源策略 */
    private String sourcePolicy;

    /** 生效开始 */
    private LocalDate effectiveFrom;

    /** 生效结束 */
    private LocalDate effectiveTo;

    /** 状态 */
    private String status;

    /** 版本 */
    private String version;

    /** 备注 */
    private String remark;
}