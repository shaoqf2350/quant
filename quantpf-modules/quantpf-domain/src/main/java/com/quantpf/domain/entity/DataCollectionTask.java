package com.quantpf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据采集任务实体（《详设_1 §7.4》）。
 * <p>对应表 {@code quantpf_data_collection_task}。</p>
 */
@Data
@TableName("quantpf_data_collection_task")
public class DataCollectionTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    @TableId(value = "pk_id", type = IdType.INPUT)
    private Long pkId;

    /** 采集任务编码 */
    private String taskCode;

    /** 数据源ID */
    private Long sourceId;

    /** 来源组ID */
    private Long sourceGroupId;

    /** 采集类型（NEWS/KLINE/CALENDAR/FINANCIAL/RULE） */
    private String taskType;

    /** 请求参数（JSON，保留原始值） */
    private String requestParams;

    /** 任务状态（CollectionTaskStatus） */
    private String status;

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

    /** 调度类型（PENDING/XXL_JOB） */
    private String schedulerType;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}