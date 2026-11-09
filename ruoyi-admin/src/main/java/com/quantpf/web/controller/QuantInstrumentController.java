package com.quantpf.web.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.quantpf.domain.vo.InstrumentVersionVo;
import com.quantpf.domain.vo.InstrumentVo;
import com.quantpf.marketdata.service.IInstrumentService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 标的身份（《详设_1 §11》：股票/期货标的台账）。
 *
 * @author QuantPF
 */
@Tag(name = "标的身份", description = "股票/期货标的台账与交易属性版本")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quant/instrument")
public class QuantInstrumentController extends BaseController {

    private final IInstrumentService instrumentService;

    /**
     * 分页查询标的。
     */
    @SaCheckPermission("quant:instrument:list")
    @Operation(summary = "分页查询标的")
    @GetMapping("/page")
    public R<PageResult<InstrumentVo>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String productType,
                                            @RequestParam(required = false) String status,
                                            PageQuery pageQuery) {
        return R.ok(instrumentService.queryPage(keyword, productType, status, pageQuery));
    }

    /**
     * 查询标的详情。
     */
    @SaCheckPermission("quant:instrument:view")
    @Operation(summary = "查询标的详情")
    @GetMapping("/{id}")
    public R<InstrumentVo> getInfo(@PathVariable Long id) {
        return R.ok(instrumentService.queryById(id));
    }

    /**
     * 查询标的交易属性版本链。
     */
    @SaCheckPermission("quant:instrument:view")
    @Operation(summary = "查询标的交易属性版本链")
    @GetMapping("/{instrumentId}/version")
    public R<List<InstrumentVersionVo>> listVersion(@PathVariable Long instrumentId) {
        return R.ok(instrumentService.listVersion(instrumentId));
    }

    /**
     * 标的同步（股票/期货基础信息，§11）。
     */
    @SaCheckPermission("quant:instrument:sync")
    @RepeatSubmit()
    @Log(title = "标的同步", businessType = BusinessType.IMPORT)
    @Operation(summary = "标的基础信息同步")
    @PostMapping("/sync")
    public R<Void> sync() {
        instrumentService.syncInstruments();
        return R.ok();
    }

    /**
     * 可交易资格判定（§12.3.4）。
     */
    @SaCheckPermission("quant:instrument:view")
    @Operation(summary = "可交易资格判定")
    @GetMapping("/tradability/{id}")
    public R<Map<String, Object>> tradability(@PathVariable Long id) {
        return R.ok(instrumentService.tradability(id));
    }
}