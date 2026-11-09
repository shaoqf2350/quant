package com.quantpf.web.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.quantpf.domain.bo.ResearchTaskBo;
import com.quantpf.domain.vo.AiResearchDraftVo;
import com.quantpf.domain.vo.EventEvidenceVo;
import com.quantpf.domain.vo.ResearchEventVo;
import com.quantpf.domain.vo.ResearchMaterialVo;
import com.quantpf.domain.vo.ResearchTaskVo;
import com.quantpf.research.service.IResearchService;
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

import java.util.List;

/**
 * 新闻研究链（《详设_1 §10》：研究任务 / Dify 草稿 / 研究事件 / 证据）。
 *
 * @author QuantPF
 */
@Tag(name = "新闻研究链", description = "研究任务、AI 草稿、研究事件与证据链")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quant/research")
public class QuantResearchController extends BaseController {

    private final IResearchService researchService;

    /**
     * 分页查询研究任务。
     */
    @SaCheckPermission("quant:news:list")
    @Operation(summary = "分页查询研究任务")
    @GetMapping("/task/page")
    public R<PageResult<ResearchTaskVo>> taskPage(@RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String taskType,
                                                  PageQuery pageQuery) {
        return R.ok(researchService.queryTaskPage(status, taskType, pageQuery));
    }

    /**
     * 创建研究任务。
     */
    @SaCheckPermission("quant:news:reanalyze")
    @RepeatSubmit()
    @Log(title = "研究任务", businessType = BusinessType.INSERT)
    @Operation(summary = "创建研究任务")
    @PostMapping("/task")
    public R<Long> createTask(@Validated @RequestBody ResearchTaskBo bo) {
        return R.ok(researchService.createTask(bo));
    }

    /**
     * 查询研究任务详情。
     */
    @SaCheckPermission("quant:news:view")
    @Operation(summary = "查询研究任务详情")
    @GetMapping("/task/{pkId}")
    public R<ResearchTaskVo> taskInfo(@PathVariable Long pkId) {
        return R.ok(researchService.queryTaskById(pkId));
    }

    /**
     * 提交任务给 Dify/TradingAgents（保存草稿、校验、生成事件并发布）。
     */
    @SaCheckPermission("quant:news:reanalyze")
    @RepeatSubmit()
    @Log(title = "研究任务", businessType = BusinessType.UPDATE)
    @Operation(summary = "提交任务给 Dify/TradingAgents 分析")
    @PostMapping("/task/{pkId}/submit")
    public R<Void> submitTask(@PathVariable Long pkId) {
        researchService.submitTask(pkId);
        return R.ok();
    }

    /**
     * 查询任务下的 AI 草稿列表。
     */
    @SaCheckPermission("quant:news:view")
    @Operation(summary = "查询任务下的 AI 草稿列表")
    @GetMapping("/task/{pkId}/draft")
    public R<List<AiResearchDraftVo>> listDraft(@PathVariable Long pkId) {
        return R.ok(researchService.listDraftByTask(pkId));
    }

    /**
     * 分页查询研究事件。
     */
    @SaCheckPermission("quant:news:list")
    @Operation(summary = "分页查询研究事件")
    @GetMapping("/event/page")
    public R<PageResult<ResearchEventVo>> eventPage(@RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String subjectType,
                                                    PageQuery pageQuery) {
        return R.ok(researchService.queryEventPage(status, subjectType, pageQuery));
    }

    /**
     * 查询研究事件详情。
     */
    @SaCheckPermission("quant:news:view")
    @Operation(summary = "查询研究事件详情")
    @GetMapping("/event/{pkId}")
    public R<ResearchEventVo> eventInfo(@PathVariable Long pkId) {
        return R.ok(researchService.queryEventById(pkId));
    }

    /**
     * 查询事件证据链。
     */
    @SaCheckPermission("quant:news:view")
    @Operation(summary = "查询事件证据链")
    @GetMapping("/event/{pkId}/evidence")
    public R<List<EventEvidenceVo>> listEvidence(@PathVariable Long pkId) {
        return R.ok(researchService.listEvidenceByEvent(pkId));
    }

    /**
     * 分页查询研究材料清单（原始快照，供创建研究任务时勾选）。
     */
    @SaCheckPermission("quant:news:list")
    @Operation(summary = "分页查询研究材料清单")
    @GetMapping("/material/page")
    public R<PageResult<ResearchMaterialVo>> materialPage(@RequestParam(required = false) String keyword,
                                                          PageQuery pageQuery) {
        return R.ok(researchService.queryMaterialPage(keyword, pageQuery));
    }
}