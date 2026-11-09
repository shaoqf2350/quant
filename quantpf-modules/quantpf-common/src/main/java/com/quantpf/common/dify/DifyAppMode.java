package com.quantpf.common.dify;

import java.util.Arrays;

/**
 * Dify 应用实际 mode（由 {@code GET /v1/info} 返回的 {@code mode} 字段解析而来）。
 *
 * <p>这是「路由」的唯一依据：不同 mode 对应不同 Service API 端点：</p>
 * <ul>
 *     <li>chat / agent-chat / advanced-chat → {@code POST /chat-messages}</li>
 *     <li>completion → {@code POST /completion-messages}</li>
 *     <li>workflow → {@code POST /workflows/run}</li>
 * </ul>
 */
public enum DifyAppMode {

    /** 聊天助手 */
    CHAT("chat"),

    /** 智能体 */
    AGENT_CHAT("agent-chat"),

    /** 对话流 */
    ADVANCED_CHAT("advanced-chat"),

    /** 文本生成 */
    COMPLETION("completion"),

    /** 工作流 */
    WORKFLOW("workflow");

    private final String mode;

    DifyAppMode(String mode) {
        this.mode = mode;
    }

    public String mode() {
        return mode;
    }

    /**
     * 解析 /v1/info 返回的 mode 字符串；无法识别时抛异常（说明 Dify 版本或应用类型未知）。
     *
     * @param mode /v1/info 返回的 mode
     * @return 对应枚举
     */
    public static DifyAppMode of(String mode) {
        if (mode == null || mode.isBlank()) {
            throw new DifyApiException("Dify /v1/info 未返回 mode 字段，无法确定应用类型");
        }
        return Arrays.stream(values())
            .filter(m -> m.mode.equalsIgnoreCase(mode))
            .findFirst()
            .orElseThrow(() -> new DifyApiException("无法识别的 Dify mode: " + mode));
    }
}
