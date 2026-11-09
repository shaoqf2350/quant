package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据源目录视图对象（《详设_1 §5.1 内置来源目录》）。
 * <p>由后端目录接口返回，供前端「新增数据源」下拉选择，前端不再硬编码来源编码。</p>
 */
@Data
public class DataSourceCatalogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 数据源编码（如 TUSHARE_PRO） */
    private String code;

    /** 目录默认名称（如 Tushare Pro） */
    private String name;

    /** 目录说明 */
    private String description;
}