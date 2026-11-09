package com.quantpf.marketdata.service;

import com.quantpf.domain.bo.SourceGroupBo;
import com.quantpf.domain.vo.SourceGroupVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.List;
import java.util.Map;

/**
 * 来源组管理服务（《详设_1 §6》）。
 * <p>除配置 CRUD 外，承担来源组交叉核验执行（§6.3）：主源/备源健康核验、主备切换、
 * 来源冲突上报（DataQualityIssue CONFLICTED）与健康状态事件发布。</p>
 */
public interface ISourceGroupService {

    /**
     * 分页查询来源组。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 来源组分页结果
     */
    PageResult<SourceGroupVo> queryPage(SourceGroupBo bo, PageQuery pageQuery);

    /**
     * 查询来源组详情。
     *
     * @param pkId 主键
     * @return 来源组详情；不存在返回 null
     */
    SourceGroupVo queryById(Long pkId);

    /**
     * 新增来源组。
     *
     * @param bo 表单
     */
    void add(SourceGroupBo bo);

    /**
     * 修改来源组。
     *
     * @param bo 表单
     */
    void edit(SourceGroupBo bo);

    /**
     * 批量删除来源组。
     *
     * @param pkIds 主键集合
     */
    void remove(List<Long> pkIds);

    /**
     * 执行来源组健康核验（§6.3 交叉核验流程）。
     * <p>确定性规则：按主源/备源配置核验数据源状态——
     * 全部 ACTIVE → HEALTHY；部分可用 → PARTIALLY_AVAILABLE（主源失败备源可用时执行主备切换）；
     * 无任何可用来源 → UNAVAILABLE；主源与既有记录冲突 → CONFLICTED 并上报 DataQualityIssue。</p>
     * <p>施工期边界（§25.2.1）：数据源均为 PENDING_CONFIRMATION（未授权），核验结果按 DEGRADED 起步，
     * 不伪造 HEALTHY；接入真实采集适配器后启用全量规则。</p>
     *
     * @param sourceGroupId 来源组ID
     * @return 核验结果（groupCode/fromStatus/toStatus/primarySwitched/availableSources/conflicts）
     */
    Map<String, Object> verifyGroupHealth(Long sourceGroupId);

    /**
     * 执行全部来源组核验（供 qualityCheckJob / 手动触发）。
     *
     * @return 各组核验结果摘要列表
     */
    List<Map<String, Object>> verifyAllGroups();
}
