package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 数据源/供应商/数据产品实体（《详设_1 §5.1》）。
 * <p>对应表 {@code quantpf_data_source}，主键 pk_id 由 Hutool Snowflake 生成（应用层填充）。</p>
 */
@Data
@TableName("quantpf_data_source")
public class DataSource implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 数据源编码（如 TUSHARE_PRO） */
    private String sourceCode;

    /** 数据源名称 */
    private String sourceName;

    /** 供应商组织名 */
    private String vendorName;

    /** 账号类型 */
    private String accountType;

    /** 合同/订阅标识 */
    private String contractRef;

    /** 服务周期与共享/商业使用范围（§5.1） */
    private String serviceScope;

    /** 费用说明（§5.1） */
    private String feeInfo;

    /** 到期日 */
    private LocalDate expiryDate;

    /** 是否主源（true 是 / false 否） */
    private Boolean primarySource;

    /** 健康状态（DataSourceStatus，与生命周期状态分开管理） */
    private String healthStatus;

    /** 生命周期状态（DataSourceStatus） */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 备注 */
    private String remark;
}