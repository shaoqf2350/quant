package com.quantpf.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据质量问题处理请求（《详设_1 §9.5》）。
 */
@Data
public class DataQualityResolveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 处理人 */
    private String handler;

    /** 处理动作 */
    @NotBlank(message = "处理动作不能为空")
    private String actionTaken;

    /** 处理依据 */
    private String basisRef;

    /** 修复版本 */
    private String fixVersion;
}