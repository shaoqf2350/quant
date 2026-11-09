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
 * 数据源字段级授权目录实体（《详设_1 §5.2》）。
 * <p>对应表 {@code quantpf_data_source_field}。</p>
 */
@Data
@TableName("quantpf_data_source_field")
public class DataSourceField implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 接口ID（关联 quantpf_data_source_interface.pk_id） */
    private Long interfaceId;

    /** 源字段名（保留原始拼写） */
    private String fieldName;

    /** 内部标准化字段名 */
    private String internalName;

    /** 数据类型/来源语义 */
    private String dataType;

    /** 授权用途（逗号分隔） */
    private String usageScope;

    /** 历史范围起始 */
    private LocalDate historyFrom;

    /** 历史范围截止 */
    private LocalDate historyTo;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}