package com.quantpf.web.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.quantpf.domain.vo.DictOptionVo;
import com.quantpf.marketdata.service.IQuantDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 枚举字典（《详设_1_补充规范_前端输入最小化与枚举字典》§三）。
 * <p>前端下拉选项的事实源：统一由后端返回 {@code [{value,label,description}]}，前端不再硬编码。</p>
 *
 * @author QuantPF
 */
@Tag(name = "枚举字典", description = "统一枚举/目录选项（前端下拉事实源）")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quant/dict")
public class QuantDictController extends BaseController {

    private final IQuantDictService dictService;

    /**
     * 查询枚举字典。
     *
     * @param enumName 字典名（枚举类或目录枚举的简单类名，如 {@code DataSourceStatus}、{@code SourceGroupCatalog}）
     * @return 选项列表
     */
    @SaCheckPermission("quant:data:query")
    @Operation(summary = "查询枚举字典")
    @GetMapping("/{enumName}")
    public R<List<DictOptionVo>> dict(@PathVariable String enumName) {
        return R.ok(dictService.listDict(enumName));
    }
}
