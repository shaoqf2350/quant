package com.quantpf.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 枚举字典选项（《详设_1_补充规范_前端输入最小化与枚举字典》§3.1 DictOptionVo）。
 * <p>由统一字典接口返回，供前端下拉选择：{@code value} 为存库值、{@code label} 为中文标签、
 * {@code description} 为可选说明。</p>
 */
@Data
public class DictOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 存库值（枚举 name() / 目录编码） */
    private String value;

    /** 中文标签 */
    private String label;

    /** 可选说明 */
    private String description;

    /** 可选排序（1 起；缺省时以列表声明顺序为准） */
    private Integer order;

    public DictOptionVo() {
    }

    public DictOptionVo(String value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    /** 无说明选项构造 */
    public static DictOptionVo of(String value, String label) {
        return new DictOptionVo(value, label, null);
    }

    /** 含说明选项构造 */
    public static DictOptionVo of(String value, String label, String description) {
        return new DictOptionVo(value, label, description);
    }

    /** 含说明与排序的选项构造 */
    public static DictOptionVo of(String value, String label, String description, Integer order) {
        DictOptionVo vo = new DictOptionVo(value, label, description);
        vo.setOrder(order);
        return vo;
    }
}
