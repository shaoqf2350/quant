package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日历来源原始快照视图对象（§12.2.6）。
 */
@Data
public class CalendarSourceSnapshotVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 来源名 */
    private String sourceName;

    /** 接口名 */
    private String endpointName;

    /** 请求参数 */
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

    /** 账号/权限说明 */
    private String permissionProfile;

    /** 创建时间 */
    private LocalDateTime createTime;
}