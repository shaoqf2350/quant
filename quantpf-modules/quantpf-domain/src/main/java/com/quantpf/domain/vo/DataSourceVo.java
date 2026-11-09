package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 数据源视图对象（§5.1，含状态中文描述）。
 */
@Data
public class DataSourceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 数据源编码 */
    private String sourceCode;

    /** 数据源名称 */
    private String sourceName;

    /** 供应商组织名 */
    private String vendorName;

    /** 账号类型 */
    private String accountType;

    /** 合同/订阅标识 */
    private String contractRef;

    /** 到期日 */
    private LocalDate expiryDate;

    /** 是否主源 */
    private Boolean primarySource;

    /** 生命周期状态 */
    private String status;

    /** 生命周期状态中文描述 */
    private String statusText;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 备注 */
    private String remark;
}