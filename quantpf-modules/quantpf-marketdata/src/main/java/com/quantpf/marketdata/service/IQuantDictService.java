package com.quantpf.marketdata.service;

import com.quantpf.domain.vo.DictOptionVo;

import java.util.List;

/**
 * 统一枚举字典服务（《详设_1_补充规范_前端输入最小化与枚举字典》§三）。
 * <p>前端下拉选项的「事实源统一在后端」：所有枚举/目录的中文标签与存库值由本服务统一返回，
 * 前端不再硬编码选项。</p>
 */
public interface IQuantDictService {

    /**
     * 查询指定枚举字典的全部选项。
     *
     * @param enumName 字典名（枚举类或目录枚举的简单类名，如 {@code DataSourceStatus}、{@code SourceGroupCatalog}）
     * @return 选项列表（value=存库值、label=中文标签、description=可选说明）；字典名未知时抛 {@code ServiceException}
     */
    List<DictOptionVo> listDict(String enumName);
}
