package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 原始数据快照实体（《详设_1 §7.2/§7.3》）。
 * <p>对应表 {@code quantpf_data_raw_snapshot}，保留原始响应与哈希。</p>
 */
@Data
@TableName("quantpf_data_raw_snapshot")
public class DataRawSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 快照业务ID */
    private String snapshotId;

    /** 数据源ID */
    private Long sourceId;

    /** 来源组ID */
    private Long sourceGroupId;

    /** 外部ID */
    private String externalId;

    /** 请求时间 */
    private LocalDateTime requestTime;

    /** 系统观察时间 */
    private LocalDateTime observedAt;

    /** 来源发布时间 */
    private LocalDateTime publishedAt;

    /** 系统可获得时间 */
    private LocalDateTime availableAt;

    /** 内容哈希（SHA-256，去重键） */
    private String contentHash;

    /** 标准化标题哈希（新闻/文档去重） */
    private String normalizedTitleHash;

    /** 原始内容引用路径 */
    private String rawContentReference;

    /** 响应状态 */
    private String responseStatus;

    /** 解析器版本 */
    private String parserVersion;

    /** 来源响应 Schema 版本 */
    private String responseSchemaVersion;

    /** 授权状态 */
    private String authorizationStatus;

    /** 幂等键 */
    private String idempotencyKey;

    /** 创建时间 */
    private LocalDateTime createTime;
}