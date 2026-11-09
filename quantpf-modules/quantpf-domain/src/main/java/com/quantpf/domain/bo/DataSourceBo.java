package com.quantpf.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 数据源业务对象（查询 + 表单，§5.1）。
 */
@Data
public class DataSourceBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（编辑时必传） */
    private Long pkId;

    /** 数据源编码 */
    @NotBlank(message = "数据源编码不能为空")
    @Size(max = 64, message = "数据源编码不能超过{max}个字符")
    private String sourceCode;

    /** 数据源名称（缺省时后端按目录默认名称兜底，少输入） */
    @Size(max = 128, message = "数据源名称不能超过{max}个字符")
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

    /** 备注 */
    private String remark;
}