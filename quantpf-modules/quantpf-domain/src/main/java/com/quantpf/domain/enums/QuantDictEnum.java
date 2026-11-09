package com.quantpf.domain.enums;

/**
 * 枚举字典统一契约（《详设_1_补充规范_前端输入最小化与枚举字典》§三）。
 * <p>所有需要暴露给前端下拉选择的枚举实现本接口：存库值默认取枚举名（{@link #value()}），
 * 中文标签取 {@link #getDesc()}，可选说明取 {@link #description()}。
 * 后端字典服务据此统一生成 {@code [{value,label,description}]}，前端不再硬编码选项。</p>
 */
public interface QuantDictEnum {

    /**
     * 存库值（下拉 value）。
     * <p>默认取枚举常量名 {@code name()}；值含特殊字符（如时区 {@code Asia/Shanghai}、周期 {@code 30M}）的枚举须覆写。</p>
     */
    default String value() {
        return ((Enum<?>) this).name();
    }

    /**
     * 中文标签（下拉 label）。
     * <p>与列表/详情中的中文文案共用同一事实源，保证「展示」与「筛选」口径一致。</p>
     */
    String getDesc();

    /**
     * 可选说明（部分枚举需要，默认 null）。
     */
    default String description() {
        return null;
    }
}
