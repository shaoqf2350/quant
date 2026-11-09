package com.quantpf.web.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.quantpf.domain.bo.CapacityAssessmentBo;
import com.quantpf.domain.bo.MarketStateBo;
import com.quantpf.domain.bo.QuantFeatureSnapshotBo;
import com.quantpf.domain.vo.CapacityAssessmentVo;
import com.quantpf.domain.vo.MarketStateVo;
import com.quantpf.domain.vo.QuantFeatureSnapshotVo;
import com.quantpf.marketdata.service.IMarketAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 市场分析（《详设_1 §14/§15/§23》：市场状态 / 容量评估 / 量价特征）。
 *
 * @author QuantPF
 */
@Tag(name = "市场分析", description = "市场状态/容量评估/量价特征快照")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quant")
public class QuantMarketAnalysisController extends BaseController {

    private final IMarketAnalysisService marketAnalysisService;

    // ============ 市场状态 ============

    @SaCheckPermission("quant:market:view")
    @Operation(summary = "分页查询市场状态")
    @GetMapping("/market/state/page")
    public R<PageResult<MarketStateVo>> marketStatePage(@RequestParam(required = false) Long instrumentId,
                                                        @RequestParam(required = false) Long calendarId,
                                                        @RequestParam(required = false) String stateCode,
                                                        PageQuery pageQuery) {
        return R.ok(marketAnalysisService.queryMarketStatePage(instrumentId, calendarId, stateCode, pageQuery));
    }

    @SaCheckPermission("quant:market:view")
    @Operation(summary = "查询市场状态详情")
    @GetMapping("/market/state/{pkId}")
    public R<MarketStateVo> marketStateInfo(@PathVariable Long pkId) {
        return R.ok(marketAnalysisService.queryMarketStateById(pkId));
    }

    @SaCheckPermission("quant:market:edit")
    @RepeatSubmit()
    @Log(title = "市场状态", businessType = BusinessType.INSERT)
    @Operation(summary = "记录市场状态")
    @PostMapping("/market/state")
    public R<Void> saveMarketState(@Validated @RequestBody MarketStateBo bo) {
        marketAnalysisService.saveMarketState(bo);
        return R.ok();
    }

    // ============ 容量评估 ============

    @SaCheckPermission("quant:market:view")
    @Operation(summary = "分页查询容量评估")
    @GetMapping("/capacity/page")
    public R<PageResult<CapacityAssessmentVo>> capacityPage(@RequestParam(required = false) Long instrumentId,
                                                            @RequestParam(required = false) String capacityStatus,
                                                            PageQuery pageQuery) {
        return R.ok(marketAnalysisService.queryCapacityPage(instrumentId, capacityStatus, pageQuery));
    }

    @SaCheckPermission("quant:market:view")
    @Operation(summary = "查询容量评估详情")
    @GetMapping("/capacity/{pkId}")
    public R<CapacityAssessmentVo> capacityInfo(@PathVariable Long pkId) {
        return R.ok(marketAnalysisService.queryCapacityById(pkId));
    }

    @SaCheckPermission("quant:market:edit")
    @RepeatSubmit()
    @Log(title = "容量评估", businessType = BusinessType.INSERT)
    @Operation(summary = "记录容量评估")
    @PostMapping("/capacity")
    public R<Void> saveCapacity(@Validated @RequestBody CapacityAssessmentBo bo) {
        marketAnalysisService.saveCapacity(bo);
        return R.ok();
    }

    // ============ 量价特征 ============

    @SaCheckPermission("quant:market:view")
    @Operation(summary = "分页查询量价特征快照")
    @GetMapping("/feature/page")
    public R<PageResult<QuantFeatureSnapshotVo>> featurePage(@RequestParam(required = false) Long instrumentId,
                                                             @RequestParam(required = false) String timeframe,
                                                             PageQuery pageQuery) {
        return R.ok(marketAnalysisService.queryFeaturePage(instrumentId, timeframe, pageQuery));
    }

    @SaCheckPermission("quant:market:view")
    @Operation(summary = "查询量价特征快照详情")
    @GetMapping("/feature/{pkId}")
    public R<QuantFeatureSnapshotVo> featureInfo(@PathVariable Long pkId) {
        return R.ok(marketAnalysisService.queryFeatureById(pkId));
    }

    @SaCheckPermission("quant:market:edit")
    @RepeatSubmit()
    @Log(title = "量价特征", businessType = BusinessType.INSERT)
    @Operation(summary = "记录量价特征快照")
    @PostMapping("/feature")
    public R<Void> saveFeature(@Validated @RequestBody QuantFeatureSnapshotBo bo) {
        marketAnalysisService.saveFeature(bo);
        return R.ok();
    }
}