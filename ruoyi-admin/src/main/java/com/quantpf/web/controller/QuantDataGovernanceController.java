package com.quantpf.web.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.quantpf.domain.vo.DataCollectionTaskVo;
import com.quantpf.domain.vo.DataPointInTimeVo;
import com.quantpf.domain.vo.DataQualityIssueVo;
import com.quantpf.domain.vo.DataRawSnapshotVo;
import com.quantpf.marketdata.service.IDataGovernanceService;
import com.quantpf.web.dto.DataQualityResolveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.log.enums.BusinessType;
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
 * 数据治理查询（《详设_1 §7/§9》：原始快照 / 采集任务 / 数据质量）。
 *
 * @author QuantPF
 */
@Tag(name = "数据治理", description = "原始快照/采集任务/数据质量问题查询与处理")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quant/data")
public class QuantDataGovernanceController extends BaseController {

    private final IDataGovernanceService dataGovernanceService;

    /**
     * 分页查询原始快照。
     */
    @SaCheckPermission("quant:data:list")
    @Operation(summary = "分页查询原始快照")
    @GetMapping("/snapshot/page")
    public R<PageResult<DataRawSnapshotVo>> snapshotPage(@RequestParam(required = false) Long sourceId,
                                                         @RequestParam(required = false) Long sourceGroupId,
                                                         PageQuery pageQuery) {
        return R.ok(dataGovernanceService.querySnapshotPage(sourceId, sourceGroupId, pageQuery));
    }

    /**
     * 查询原始快照详情。
     */
    @SaCheckPermission("quant:data:query")
    @Operation(summary = "查询原始快照详情")
    @GetMapping("/snapshot/{pkId}")
    public R<DataRawSnapshotVo> snapshotInfo(@PathVariable Long pkId) {
        return R.ok(dataGovernanceService.querySnapshotById(pkId));
    }

    /**
     * 分页查询采集任务。
     */
    @SaCheckPermission("quant:data:list")
    @Operation(summary = "分页查询采集任务")
    @GetMapping("/collection/page")
    public R<PageResult<DataCollectionTaskVo>> collectionPage(@RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String taskType,
                                                              PageQuery pageQuery) {
        return R.ok(dataGovernanceService.queryCollectionPage(status, taskType, pageQuery));
    }

    /**
     * 分页查询数据质量问题。
     */
    @SaCheckPermission("quant:data:list")
    @Operation(summary = "分页查询数据质量问题")
    @GetMapping("/quality/page")
    public R<PageResult<DataQualityIssueVo>> qualityPage(@RequestParam(required = false) String issueStatus,
                                                         @RequestParam(required = false) String qualityStatus,
                                                         PageQuery pageQuery) {
        return R.ok(dataGovernanceService.queryQualityPage(issueStatus, qualityStatus, pageQuery));
    }

    /**
     * 处理（解决）数据质量问题。
     */
    @SaCheckPermission("quant:data:resolve")
    @RepeatSubmit()
    @Log(title = "数据质量处理", businessType = BusinessType.UPDATE)
    @Operation(summary = "处理数据质量问题")
    @PostMapping("/quality/{pkId}/resolve")
    public R<Void> resolveQuality(@PathVariable Long pkId, @Validated @RequestBody DataQualityResolveRequest request) {
        dataGovernanceService.resolveQuality(pkId, request.getHandler(), request.getActionTaken(),
            request.getBasisRef(), request.getFixVersion());
        return R.ok();
    }

    /**
     * 分页查询点时数据版本。
     */
    @SaCheckPermission("quant:data:list")
    @Operation(summary = "分页查询点时数据版本")
    @GetMapping("/point-in-time/page")
    public R<PageResult<DataPointInTimeVo>> pointInTimePage(@RequestParam(required = false) String subjectType,
                                                            @RequestParam(required = false) Long subjectId,
                                                            PageQuery pageQuery) {
        return R.ok(dataGovernanceService.queryPointInTimePage(subjectType, subjectId, pageQuery));
    }

    /**
     * 查询点时数据版本详情。
     */
    @SaCheckPermission("quant:data:query")
    @Operation(summary = "查询点时数据版本详情")
    @GetMapping("/point-in-time/{pkId}")
    public R<DataPointInTimeVo> pointInTimeInfo(@PathVariable Long pkId) {
        return R.ok(dataGovernanceService.queryPointInTimeById(pkId));
    }
}