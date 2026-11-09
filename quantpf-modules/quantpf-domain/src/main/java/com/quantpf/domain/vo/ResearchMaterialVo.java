package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 研究材料清单视图对象（《详设_1 §10》）。
 * <p>「材料」即已采集落库的原始快照 {@code quantpf_data_raw_snapshot}，
 * 供创建研究任务时勾选（material_ids 引用其主键 pk_id），并回填数据源友好名称。</p>
 */
@Data
public class ResearchMaterialVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 材料ID（原始快照主键，研究任务 material_ids 引用此值） */
    private Long pkId;

    /** 快照业务ID */
    private String snapshotId;

    /** 外部ID（新闻/公告/研报在来源方的标识） */
    private String externalId;

    /** 数据源ID */
    private Long sourceId;

    /** 数据源编码 */
    private String sourceCode;

    /** 数据源名称 */
    private String sourceName;

    /** 来源组ID */
    private Long sourceGroupId;

    /** 来源发布时间 */
    private LocalDateTime publishedAt;

    /** 系统观察时间 */
    private LocalDateTime observedAt;

    /** 授权状态 */
    private String authorizationStatus;
}
