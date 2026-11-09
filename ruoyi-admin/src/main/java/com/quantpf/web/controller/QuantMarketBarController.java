package com.quantpf.web.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.quantpf.domain.bo.MarketBarBo;
import com.quantpf.domain.vo.MarketBarVo;
import com.quantpf.marketdata.service.IMarketBarService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * K 线行情（《详设_1 §14》）。
 *
 * @author QuantPF
 */
@Tag(name = "K线行情", description = "K线分页查询与增量导入")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quant/market/bar")
public class QuantMarketBarController extends BaseController {

    private final IMarketBarService marketBarService;

    @SaCheckPermission("quant:kline:list")
    @Operation(summary = "分页查询K线")
    @GetMapping("/page")
    public R<PageResult<MarketBarVo>> page(@RequestParam(required = false) Long instrumentId,
                                           @RequestParam(required = false) String timeframe,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                           PageQuery pageQuery) {
        return R.ok(marketBarService.queryBarPage(instrumentId, timeframe, startTime, endTime, pageQuery));
    }

    @SaCheckPermission("quant:kline:list")
    @Operation(summary = "查询单条K线详情")
    @GetMapping("/{id}")
    public R<MarketBarVo> info(@PathVariable Long id) {
        return R.ok(marketBarService.queryBarById(id));
    }

    @SaCheckPermission("quant:kline:import")
    @RepeatSubmit()
    @Log(title = "K线导入", businessType = BusinessType.INSERT)
    @Operation(summary = "K线增量导入")
    @PostMapping("/import")
    public R<Void> importBar(@Validated @RequestBody MarketBarBo bo) {
        marketBarService.importBar(bo);
        return R.ok();
    }
}