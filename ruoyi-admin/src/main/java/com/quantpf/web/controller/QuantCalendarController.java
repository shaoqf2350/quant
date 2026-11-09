package com.quantpf.web.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.quantpf.domain.bo.CalendarDefBo;
import com.quantpf.domain.vo.CalendarDayVo;
import com.quantpf.domain.vo.CalendarDefVo;
import com.quantpf.domain.vo.CalendarSessionVo;
import com.quantpf.domain.vo.InstrumentCalendarBindingVo;
import com.quantpf.domain.vo.MarketDataReadinessVo;
import com.quantpf.domain.vo.CalendarConflictVo;
import com.quantpf.domain.vo.CalendarSourceSnapshotVo;
import com.quantpf.domain.vo.FxSessionRuleVo;
import com.quantpf.marketdata.service.ICalendarService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 交易日历与日线就绪（《详设_1 §12》）。
 *
 * @author QuantPF
 */
@Tag(name = "交易日历", description = "日历定义/交易日/交易时段/标的绑定/日线就绪")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quant/calendar")
public class QuantCalendarController extends BaseController {

    private final ICalendarService calendarService;

    /**
     * 分页查询日历定义。
     */
    @SaCheckPermission("quant:calendar:list")
    @Operation(summary = "分页查询日历定义")
    @GetMapping("/page")
    public R<PageResult<CalendarDefVo>> page(CalendarDefBo bo, PageQuery pageQuery) {
        return R.ok(calendarService.queryPage(bo, pageQuery));
    }

    /**
     * 查询日历定义详情。
     */
    @SaCheckPermission("quant:calendar:list")
    @Operation(summary = "查询日历定义详情")
    @GetMapping("/{pkId}")
    public R<CalendarDefVo> getInfo(@PathVariable Long pkId) {
        return R.ok(calendarService.queryById(pkId));
    }

    /**
     * 新增日历定义。
     */
    @SaCheckPermission("quant:calendar:import")
    @RepeatSubmit()
    @Log(title = "日历定义", businessType = BusinessType.INSERT)
    @Operation(summary = "新增日历定义")
    @PostMapping
    public R<Void> add(@Validated @RequestBody CalendarDefBo bo) {
        calendarService.add(bo);
        return R.ok();
    }

    /**
     * 修改日历定义。
     */
    @SaCheckPermission("quant:calendar:import")
    @RepeatSubmit()
    @Log(title = "日历定义", businessType = BusinessType.UPDATE)
    @Operation(summary = "修改日历定义")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody CalendarDefBo bo) {
        calendarService.edit(bo);
        return R.ok();
    }

    /**
     * 批量删除日历定义。
     */
    @SaCheckPermission("quant:calendar:import")
    @RepeatSubmit()
    @Log(title = "日历定义", businessType = BusinessType.DELETE)
    @Operation(summary = "批量删除日历定义")
    @DeleteMapping("/{pkIds}")
    public R<Void> remove(@PathVariable Long[] pkIds) {
        calendarService.remove(Arrays.asList(pkIds));
        return R.ok();
    }

    /**
     * 分页查询交易日记录。
     */
    @SaCheckPermission("quant:calendar:list")
    @Operation(summary = "分页查询交易日记录")
    @GetMapping("/day/page")
    public R<PageResult<CalendarDayVo>> dayPage(@RequestParam(required = false) Long calendarId,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                PageQuery pageQuery) {
        return R.ok(calendarService.queryDayPage(calendarId, startDate, endDate, pageQuery));
    }

    /**
     * 查询某市场本地日期的交易日详情（含前后交易日）。
     */
    @SaCheckPermission("quant:calendar:list")
    @Operation(summary = "查询交易日详情（含前后交易日）")
    @GetMapping("/day/detail")
    public R<CalendarDayVo> dayDetail(@RequestParam Long calendarId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(calendarService.queryDayDetail(calendarId, date));
    }

    /**
     * 查询日历下的交易时段模板。
     */
    @SaCheckPermission("quant:calendar:sessions")
    @Operation(summary = "查询交易时段模板")
    @GetMapping("/session/list")
    public R<List<CalendarSessionVo>> listSession(@RequestParam Long calendarId) {
        return R.ok(calendarService.listSession(calendarId));
    }

    /**
     * 查询标的/日历绑定。
     */
    @SaCheckPermission("quant:calendar:list")
    @Operation(summary = "查询标的/日历绑定")
    @GetMapping("/binding/list")
    public R<List<InstrumentCalendarBindingVo>> listBinding(@RequestParam(required = false) Long instrumentId,
                                                            @RequestParam(required = false) Long calendarId) {
        return R.ok(calendarService.listBinding(instrumentId, calendarId));
    }

    /**
     * 分页查询日线数据就绪状态。
     */
    @SaCheckPermission("quant:calendar:list")
    @Operation(summary = "分页查询日线数据就绪状态")
    @GetMapping("/readiness/page")
    public R<PageResult<MarketDataReadinessVo>> readinessPage(@RequestParam(required = false) Long calendarId,
                                                               @RequestParam(required = false) String blockReason,
                                                               PageQuery pageQuery) {
        return R.ok(calendarService.queryReadinessPage(calendarId, blockReason, pageQuery));
    }

    /**
     * 分页查询日历来源原始快照（§12.2.6 专业详情）。
     */
    @SaCheckPermission("quant:calendar:list")
    @Operation(summary = "分页查询日历来源原始快照")
    @GetMapping("/source-snapshot/page")
    public R<PageResult<CalendarSourceSnapshotVo>> sourceSnapshotPage(@RequestParam(required = false) String sourceName,
                                                                      @RequestParam(required = false) String endpointName,
                                                                      PageQuery pageQuery) {
        return R.ok(calendarService.querySourceSnapshotPage(sourceName, endpointName, pageQuery));
    }

    /**
     * 分页查询日历冲突。
     */
    @SaCheckPermission("quant:calendar:list")
    @Operation(summary = "分页查询日历冲突")
    @GetMapping("/conflict/page")
    public R<PageResult<CalendarConflictVo>> conflictPage(@RequestParam(required = false) Long calendarId,
                                                          @RequestParam(required = false) String status,
                                                          PageQuery pageQuery) {
        return R.ok(calendarService.queryConflictPage(calendarId, status, pageQuery));
    }

    /**
     * 人工覆盖日历冲突（§12.2.2）。
     */
    @SaCheckPermission("quant:calendar:import")
    @RepeatSubmit()
    @Log(title = "日历冲突", businessType = BusinessType.UPDATE)
    @Operation(summary = "人工覆盖日历冲突")
    @PostMapping("/conflict/{pkId}/override")
    public R<Void> overrideConflict(@PathVariable Long pkId, @RequestParam String overrideDecision) {
        calendarService.overrideConflict(pkId, overrideDecision);
        return R.ok();
    }

    /**
     * 导入交易日历（§12.2 Tushare 适配入口；施工期 Mock，冲突不静默择一）。
     */
    @SaCheckPermission("quant:calendar:import")
    @RepeatSubmit()
    @Log(title = "交易日历导入", businessType = BusinessType.IMPORT)
    @Operation(summary = "导入交易日历（trade_cal/fut_trade_cal/hk_tradecal/us_tradecal，Mock）")
    @PostMapping("/{pkId}/import")
    public R<java.util.Map<String, Object>> importCalendar(@PathVariable Long pkId,
                                                           @RequestParam String endpoint,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(calendarService.importCalendar(pkId, endpoint, startDate, endDate));
    }

    /**
     * 分页查询外汇时段规则（§12.2.5，一期 schema 预留）。
     */
    @SaCheckPermission("quant:calendar:list")
    @Operation(summary = "分页查询外汇时段规则")
    @GetMapping("/fx-session-rule/page")
    public R<PageResult<FxSessionRuleVo>> fxSessionRulePage(@RequestParam(required = false) String classify,
                                                            PageQuery pageQuery) {
        return R.ok(calendarService.queryFxSessionRulePage(classify, pageQuery));
    }
}