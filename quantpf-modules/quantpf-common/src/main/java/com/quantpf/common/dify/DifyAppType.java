package com.quantpf.common.dify;

import java.util.Arrays;

/**
 * Dify 应用类型枚举，代表「调用方的业务意图」。
 *
 * <p>它与 Dify 侧实际的 {@code mode}（由 {@code GET /v1/info} 返回）是两个维度：</p>
 * <ul>
 *     <li>{@link #getCode() code}：配置键，对应 {@code quantpf.dify.apps.<code>.api-key}，用于定位要调用的 Dify 应用；</li>
 *     <li>{@link #getExpectedMode() expectedMode}：该业务意图在 Dify 侧<strong>应当</strong>具备的 mode，仅用于
 *     「应用与 apiKey 是否配对」的校验，<strong>绝不用于路由</strong>。</li>
 * </ul>
 *
 * <p>路由一律以 {@code GET /v1/info} 返回的实际 mode 为准（见 {@link DifyAppMode}），
 * 禁止在本枚举上硬编码端点路由。</p>
 */
public enum DifyAppType {

    /** 聊天助手（Dify mode: chat） */
    CHAT("chat", DifyAppMode.CHAT, "聊天助手 (chat)"),

    /** 智能体（Dify mode: agent-chat） */
    AGENT("agent", DifyAppMode.AGENT_CHAT, "智能体 (agent-chat)"),

    /** 对话流（Dify mode: advanced-chat） */
    CHATFLOW("chatflow", DifyAppMode.ADVANCED_CHAT, "对话流 (advanced-chat)"),

    /** 工作流（Dify mode: workflow） */
    WORKFLOW("workflow", DifyAppMode.WORKFLOW, "工作流 (workflow)"),

    /** 文本生成（Dify mode: completion） */
    TEXT_GENERATION("text-generation", DifyAppMode.COMPLETION, "文本生成 (completion)");

    /** 配置键：quantpf.dify.apps.<code>.api-key */
    private final String code;

    /** 配对校验用：/v1/info 应返回的 mode，绝不用于路由 */
    private final DifyAppMode expectedMode;

    /** 中文描述 */
    private final String description;

    DifyAppType(String code, DifyAppMode expectedMode, String description) {
        this.code = code;
        this.expectedMode = expectedMode;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public DifyAppMode getExpectedMode() {
        return expectedMode;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按配置键解析枚举；未知键抛异常（快速失败，避免静默拿不到应用配置）。
     *
     * @param code 配置键
     * @return 对应枚举
     */
    public static DifyAppType ofCode(String code) {
        return Arrays.stream(values())
            .filter(t -> t.code.equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知的 Dify 应用类型配置键: " + code));
    }
}
