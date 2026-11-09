package com.quantpf.marketdata.service;

import com.quantpf.domain.vo.DataCollectionTaskVo;
import com.quantpf.domain.vo.DataPointInTimeVo;
import com.quantpf.domain.vo.DataQualityIssueVo;
import com.quantpf.domain.vo.DataRawSnapshotVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

/**
 * 数据治理服务（《详设_1 §7/§9》：原始快照 / 采集任务 / 数据质量）。
 */
public interface IDataGovernanceService {

    /**
     * 分页查询原始快照。
     *
     * @param sourceId      数据源ID（可空）
     * @param sourceGroupId 来源组ID（可空）
     * @param pageQuery     分页参数
     * @return 原始快照分页结果
     */
    PageResult<DataRawSnapshotVo> querySnapshotPage(Long sourceId, Long sourceGroupId, PageQuery pageQuery);

    /**
     * 查询原始快照详情。
     *
     * @param pkId 主键
     * @return 原始快照详情；不存在返回 null
     */
    DataRawSnapshotVo querySnapshotById(Long pkId);

    /**
     * 分页查询采集任务。
     *
     * @param status    任务状态（可空）
     * @param taskType  采集类型（可空）
     * @param pageQuery 分页参数
     * @return 采集任务分页结果
     */
    PageResult<DataCollectionTaskVo> queryCollectionPage(String status, String taskType, PageQuery pageQuery);

    /**
     * 分页查询数据质量问题。
     *
     * @param issueStatus   问题生命周期状态（可空）
     * @param qualityStatus 数据质量状态（可空）
     * @param pageQuery     分页参数
     * @return 数据质量问题分页结果
     */
    PageResult<DataQualityIssueVo> queryQualityPage(String issueStatus, String qualityStatus, PageQuery pageQuery);

    /**
     * 处理（解决）数据质量问题（《详设_1 §9.5》）。
     *
     * @param pkId        问题主键
     * @param handler     处理人
     * @param actionTaken 处理动作
     * @param basisRef    处理依据
     * @param fixVersion  修复版本
     */
    void resolveQuality(Long pkId, String handler, String actionTaken, String basisRef, String fixVersion);

    /**
     * 分页查询点时数据版本。
     */
    PageResult<DataPointInTimeVo> queryPointInTimePage(String subjectType, Long subjectId, PageQuery pageQuery);

    /**
     * 查询点时数据版本详情。
     */
    DataPointInTimeVo queryPointInTimeById(Long pkId);
}