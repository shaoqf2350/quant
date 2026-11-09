package com.quantpf.web.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.quantpf.domain.bo.CorporateActionBo;
import com.quantpf.domain.bo.ContractEventBo;
import com.quantpf.domain.vo.CorporateActionVo;
import com.quantpf.domain.vo.ContractEventVo;
import com.quantpf.marketdata.service.IContractEventService;
import com.quantpf.marketdata.service.ICorporateActionService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * 公司行为与期货合约事件（《详设_1 §13》）。
 *
 * @author QuantPF
 */
@Tag(name = "公司行为与合约事件", description = "股票公司行为与期货合约生命周期/制度事件")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quant/event")
public class QuantEventController extends BaseController {

    private final ICorporateActionService corporateActionService;
    private final IContractEventService contractEventService;

    // ============ 公司行为 ============

    @SaCheckPermission("quant:instrument:list")
    @Operation(summary = "分页查询公司行为")
    @GetMapping("/corporate/page")
    public R<PageResult<CorporateActionVo>> corporatePage(CorporateActionBo bo, PageQuery pageQuery) {
        return R.ok(corporateActionService.queryPage(bo, pageQuery));
    }

    @SaCheckPermission("quant:instrument:view")
    @Operation(summary = "查询公司行为详情")
    @GetMapping("/corporate/{pkId}")
    public R<CorporateActionVo> corporateInfo(@PathVariable Long pkId) {
        return R.ok(corporateActionService.queryById(pkId));
    }

    @SaCheckPermission("quant:instrument:edit")
    @RepeatSubmit()
    @Log(title = "公司行为", businessType = BusinessType.INSERT)
    @Operation(summary = "新增公司行为")
    @PostMapping("/corporate")
    public R<Void> addCorporate(@Validated @RequestBody CorporateActionBo bo) {
        corporateActionService.add(bo);
        return R.ok();
    }

    @SaCheckPermission("quant:instrument:edit")
    @RepeatSubmit()
    @Log(title = "公司行为", businessType = BusinessType.UPDATE)
    @Operation(summary = "修改公司行为")
    @PutMapping("/corporate")
    public R<Void> editCorporate(@Validated @RequestBody CorporateActionBo bo) {
        corporateActionService.edit(bo);
        return R.ok();
    }

    @SaCheckPermission("quant:instrument:edit")
    @RepeatSubmit()
    @Log(title = "公司行为", businessType = BusinessType.DELETE)
    @Operation(summary = "批量删除公司行为")
    @DeleteMapping("/corporate/{pkIds}")
    public R<Void> removeCorporate(@PathVariable Long[] pkIds) {
        corporateActionService.remove(Arrays.asList(pkIds));
        return R.ok();
    }

    // ============ 期货合约事件 ============

    @SaCheckPermission("quant:instrument:list")
    @Operation(summary = "分页查询合约事件")
    @GetMapping("/contract/page")
    public R<PageResult<ContractEventVo>> contractPage(ContractEventBo bo, PageQuery pageQuery) {
        return R.ok(contractEventService.queryPage(bo, pageQuery));
    }

    @SaCheckPermission("quant:instrument:view")
    @Operation(summary = "查询合约事件详情")
    @GetMapping("/contract/{pkId}")
    public R<ContractEventVo> contractInfo(@PathVariable Long pkId) {
        return R.ok(contractEventService.queryById(pkId));
    }

    @SaCheckPermission("quant:instrument:edit")
    @RepeatSubmit()
    @Log(title = "合约事件", businessType = BusinessType.INSERT)
    @Operation(summary = "新增合约事件")
    @PostMapping("/contract")
    public R<Void> addContract(@Validated @RequestBody ContractEventBo bo) {
        contractEventService.add(bo);
        return R.ok();
    }

    @SaCheckPermission("quant:instrument:edit")
    @RepeatSubmit()
    @Log(title = "合约事件", businessType = BusinessType.UPDATE)
    @Operation(summary = "修改合约事件")
    @PutMapping("/contract")
    public R<Void> editContract(@Validated @RequestBody ContractEventBo bo) {
        contractEventService.edit(bo);
        return R.ok();
    }

    @SaCheckPermission("quant:instrument:edit")
    @RepeatSubmit()
    @Log(title = "合约事件", businessType = BusinessType.DELETE)
    @Operation(summary = "批量删除合约事件")
    @DeleteMapping("/contract/{pkIds}")
    public R<Void> removeContract(@PathVariable Long[] pkIds) {
        contractEventService.remove(Arrays.asList(pkIds));
        return R.ok();
    }
}