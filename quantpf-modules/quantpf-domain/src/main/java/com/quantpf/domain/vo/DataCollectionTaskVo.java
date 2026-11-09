package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据采集任务视图对象（§7.4，含状态中文描述）。
 */
@Data
public class DataCollectionTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long pkId;

    /** 采集任务编码 */
    private String taskCode;

    /** 数据源ID */
    private Long sourceId;

    /** 来源组ID */
    private Long sourceGroupId;

    /** 采集类型 */
    private String taskType;

    /** 请求参数 */
    private String requestParams;

    /** 任务状态 */
    private String status;

    /** 任务状态中文描述 */
    private String statusText;

    /** 成功范围 */
    private String successScope;

    /** 失败范围 */
    private String failureScope;

    /** 异常码 */
    private String errorCode;

    /** 重试次数 */
    private Integer retryCount;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 结束时间 */
    private LocalDateTime finishedAt;

    /** 调度类型 */
    private String schedulerType;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}