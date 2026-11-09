package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 来源组与交叉核验配置实体（《详设_1 §6》）。
 * <p>对应表 {@code quantpf_source_group}。</p>
 */
@Data
@TableName("quantpf_source_group")
public class SourceGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 来源组编码（NEWS_POLICY/MARKET_PRICE/TRADING_RULE...） */
    private String groupCode;

    /** 来源组名称 */
    private String groupName;

    /** 主源数据源ID */
    private Long primarySourceId;

    /** 备源数据源ID（逗号分隔） */
    private String backupSourceIds;

    /** 交叉核验规则 */
    private String verifyRule;

    /** 转载/去重识别策略 */
    private String reprintPolicy;

    /** 来源组状态（SourceGroupStatus） */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}