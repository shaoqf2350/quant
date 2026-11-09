package com.quantpf.marketdata.service;

import com.quantpf.domain.bo.CapacityAssessmentBo;
import com.quantpf.domain.bo.MarketStateBo;
import com.quantpf.domain.bo.QuantFeatureSnapshotBo;
import com.quantpf.domain.vo.CapacityAssessmentVo;
import com.quantpf.domain.vo.MarketStateVo;
import com.quantpf.domain.vo.QuantFeatureSnapshotVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

/**
 * 市场分析服务（《详设_1 §14/§15/§23》：市场状态 / 容量评估 / 量价特征）。
 */
public interface IMarketAnalysisService {

    /**
     * 分页查询市场状态。
     */
    PageResult<MarketStateVo> queryMarketStatePage(Long instrumentId, Long calendarId, String stateCode, PageQuery pageQuery);

    /**
     * 查询市场状态详情。
     */
    MarketStateVo queryMarketStateById(Long pkId);

    /**
     * 记录市场状态（供 XXL-JOB / 人工维护调用）。
     */
    void saveMarketState(MarketStateBo bo);

    /**
     * 分页查询容量评估。
     */
    PageResult<CapacityAssessmentVo> queryCapacityPage(Long instrumentId, String capacityStatus, PageQuery pageQuery);

    /**
     * 查询容量评估详情。
     */
    CapacityAssessmentVo queryCapacityById(Long pkId);

    /**
     * 记录容量评估。
     */
    void saveCapacity(CapacityAssessmentBo bo);

    /**
     * 分页查询量价特征快照。
     */
    PageResult<QuantFeatureSnapshotVo> queryFeaturePage(Long instrumentId, String timeframe, PageQuery pageQuery);

    /**
     * 查询量价特征快照详情。
     */
    QuantFeatureSnapshotVo queryFeatureById(Long pkId);

    /**
     * 记录量价特征快照（二期量价特征计算预留）。
     */
    void saveFeature(QuantFeatureSnapshotBo bo);
}