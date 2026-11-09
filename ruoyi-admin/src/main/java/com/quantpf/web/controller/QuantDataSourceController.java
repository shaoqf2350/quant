package com.quantpf.web.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.quantpf.domain.bo.DataSourceBo;
import com.quantpf.domain.bo.SourceGroupBo;
import com.quantpf.domain.entity.DataSourceField;
import com.quantpf.domain.entity.DataSourceInterface;
import com.quantpf.domain.vo.DataSourceCatalogVo;
import com.quantpf.domain.vo.DataSourceVo;
import com.quantpf.domain.vo.SourceGroupVo;
import com.quantpf.marketdata.service.IDataSourceService;
import com.quantpf.marketdata.service.ISourceGroupService;
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
import java.util.List;

/**
 * 数据源管理（《详设_1 §5/§6》：数据源 / 来源组 / 接口目录 / 字段目录）。
 *
 * @author QuantPF
 */
@Tag(name = "数据源管理", description = "数据源/供应商/来源组配置与授权")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quant/data/source")
public class QuantDataSourceController extends BaseController {

    private final IDataSourceService dataSourceService;
    private final ISourceGroupService sourceGroupService;

    /**
     * 内置数据源目录（§25.2.1，事实源在后端；供「新增数据源」下拉选择）。
     */
    @SaCheckPermission("quant:data:list")
    @Operation(summary = "内置数据源目录")
    @GetMapping("/catalog")
    public R<List<DataSourceCatalogVo>> catalog() {
        return R.ok(dataSourceService.listCatalog());
    }

    /**
     * 分页查询数据源。
     */
    @SaCheckPermission("quant:data:list")
    @Operation(summary = "分页查询数据源")
    @GetMapping("/page")
    public R<PageResult<DataSourceVo>> page(DataSourceBo bo, PageQuery pageQuery) {
        return R.ok(dataSourceService.queryPage(bo, pageQuery));
    }

    /**
     * 查询数据源详情。
     */
    @SaCheckPermission("quant:data:query")
    @Operation(summary = "查询数据源详情")
    @GetMapping("/{pkId}")
    public R<DataSourceVo> getInfo(@PathVariable Long pkId) {
        return R.ok(dataSourceService.queryById(pkId));
    }

    /**
     * 新增数据源。
     */
    @SaCheckPermission("quant:data:add")
    @RepeatSubmit()
    @Log(title = "数据源管理", businessType = BusinessType.INSERT)
    @Operation(summary = "新增数据源")
    @PostMapping
    public R<Void> add(@Validated @RequestBody DataSourceBo bo) {
        dataSourceService.add(bo);
        return R.ok();
    }

    /**
     * 修改数据源。
     */
    @SaCheckPermission("quant:data:edit")
    @RepeatSubmit()
    @Log(title = "数据源管理", businessType = BusinessType.UPDATE)
    @Operation(summary = "修改数据源")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody DataSourceBo bo) {
        dataSourceService.edit(bo);
        return R.ok();
    }

    /**
     * 批量删除数据源。
     */
    @SaCheckPermission("quant:data:remove")
    @RepeatSubmit()
    @Log(title = "数据源管理", businessType = BusinessType.DELETE)
    @Operation(summary = "批量删除数据源")
    @DeleteMapping("/{pkIds}")
    public R<Void> remove(@PathVariable Long[] pkIds) {
        dataSourceService.remove(Arrays.asList(pkIds));
        return R.ok();
    }

    /**
     * 查询数据源下的接口目录。
     */
    @SaCheckPermission("quant:data:query")
    @Operation(summary = "查询数据源接口目录")
    @GetMapping("/{sourceId}/interface")
    public R<List<DataSourceInterface>> listInterface(@PathVariable Long sourceId) {
        return R.ok(dataSourceService.listInterface(sourceId));
    }

    /**
     * 查询接口下的字段级授权目录。
     */
    @SaCheckPermission("quant:data:query")
    @Operation(summary = "查询接口字段目录")
    @GetMapping("/interface/{interfaceId}/field")
    public R<List<DataSourceField>> listField(@PathVariable Long interfaceId) {
        return R.ok(dataSourceService.listField(interfaceId));
    }

    /**
     * 分页查询来源组。
     */
    @SaCheckPermission("quant:data:list")
    @Operation(summary = "分页查询来源组")
    @GetMapping("/group/page")
    public R<PageResult<SourceGroupVo>> groupPage(SourceGroupBo bo, PageQuery pageQuery) {
        return R.ok(sourceGroupService.queryPage(bo, pageQuery));
    }

    /**
     * 查询来源组详情。
     */
    @SaCheckPermission("quant:data:query")
    @Operation(summary = "查询来源组详情")
    @GetMapping("/group/{pkId}")
    public R<SourceGroupVo> groupInfo(@PathVariable Long pkId) {
        return R.ok(sourceGroupService.queryById(pkId));
    }

    /**
     * 新增来源组。
     */
    @SaCheckPermission("quant:data:add")
    @RepeatSubmit()
    @Log(title = "来源组管理", businessType = BusinessType.INSERT)
    @Operation(summary = "新增来源组")
    @PostMapping("/group")
    public R<Void> addGroup(@Validated @RequestBody SourceGroupBo bo) {
        sourceGroupService.add(bo);
        return R.ok();
    }

    /**
     * 修改来源组。
     */
    @SaCheckPermission("quant:data:edit")
    @RepeatSubmit()
    @Log(title = "来源组管理", businessType = BusinessType.UPDATE)
    @Operation(summary = "修改来源组")
    @PutMapping("/group")
    public R<Void> editGroup(@Validated @RequestBody SourceGroupBo bo) {
        sourceGroupService.edit(bo);
        return R.ok();
    }

    /**
     * 批量删除来源组。
     */
    @SaCheckPermission("quant:data:remove")
    @RepeatSubmit()
    @Log(title = "来源组管理", businessType = BusinessType.DELETE)
    @Operation(summary = "批量删除来源组")
    @DeleteMapping("/group/{pkIds}")
    public R<Void> removeGroup(@PathVariable Long[] pkIds) {
        sourceGroupService.remove(Arrays.asList(pkIds));
        return R.ok();
    }

    /**
     * 执行来源组健康核验（§6.3 主备切换/冲突上报/状态流转）。
     */
    @SaCheckPermission("quant:data:edit")
    @RepeatSubmit()
    @Log(title = "来源组核验", businessType = BusinessType.OTHER)
    @Operation(summary = "执行来源组健康核验（主备切换/冲突上报）")
    @PostMapping("/group/{pkId}/verify")
    public R<java.util.Map<String, Object>> verifyGroup(@PathVariable Long pkId) {
        return R.ok(sourceGroupService.verifyGroupHealth(pkId));
    }

    /**
     * 执行全部来源组核验（供手动触发；调度由 qualityCheckJob 周期执行）。
     */
    @SaCheckPermission("quant:data:edit")
    @RepeatSubmit()
    @Log(title = "来源组核验", businessType = BusinessType.OTHER)
    @Operation(summary = "执行全部来源组健康核验")
    @PostMapping("/group/verify-all")
    public R<java.util.List<java.util.Map<String, Object>>> verifyAllGroups() {
        return R.ok(sourceGroupService.verifyAllGroups());
    }
}