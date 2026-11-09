package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日历来源原始快照实体（《详设_1 §12.2.6》）。
 * <p>对应表 {@code quantpf_calendar_source_snapshot}，保留每个接口原始请求/响应。</p>
 */
@Data
@TableName("quantpf_calendar_source_snapshot")
public class CalendarSourceSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 来源名 */
    private String sourceName;

    /** 接口名 */
    private String endpointName;

    /** 请求参数（JSON） */
    private String requestParams;

    /** 响应 Schema 版本 */
    private String responseSchemaVersion;

    /** 原始载荷哈希 */
    private String rawPayloadHash;

    /** 原始载荷引用 */
    private String rawPayloadRef;

    /** 抓取时间 */
    private LocalDateTime retrievedAt;

    /** 来源观察时间 */
    private LocalDateTime sourceObservedAt;

    /** 账号/权限说明（不含 Token 明文） */
    private String permissionProfile;

    /** 创建时间 */
    private LocalDateTime createTime;
}