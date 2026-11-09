package com.quantpf.common.dify;

/**
 * 一条已解析的 SSE 事件。
 *
 * <p>由 {@link SseEventParser} 解析 Dify 流式响应产生。Dify 的 SSE 事件形如：</p>
 * <pre>
 * event: message
 * data: {"event":"message","answer":"你好"}
 * </pre>
 * 其中 {@link #event()} 即 {@code event:} 字段（事件类型），{@link #data()} 即 {@code data:} 字段（JSON 载荷）。
 *
 * @param event 事件类型（message / message_end / workflow_finished / error / ping …），未显式声明时默认为 "message"
 * @param data  事件载荷（通常是 JSON 字符串，多行 data 会以 {@code \n} 拼接）
 * @param id    事件 id（SSE {@code id:} 字段，无则为 null）
 * @param retry 重连间隔毫秒（SSE {@code retry:} 字段，无则为 -1）
 */
public record DifySseEvent(String event, String data, String id, long retry) {

    public static final String EVENT_MESSAGE = "message";
    public static final String EVENT_MESSAGE_END = "message_end";
    public static final String EVENT_MESSAGE_REPLACE = "message_replace";
    public static final String EVENT_AGENT_MESSAGE = "agent_message";
    public static final String EVENT_AGENT_THOUGHT = "agent_thought";
    public static final String EVENT_ERROR = "error";
    public static final String EVENT_PING = "ping";
    public static final String EVENT_WORKFLOW_STARTED = "workflow_started";
    public static final String EVENT_NODE_STARTED = "node_started";
    public static final String EVENT_NODE_FINISHED = "node_finished";
    public static final String EVENT_WORKFLOW_FINISHED = "workflow_finished";

    /** OpenAI 风格流结束哨兵。Dify 不使用（Dify 以 message_end / workflow_finished 结束），仅做通用兼容 */
    public static final String DATA_DONE = "[DONE]";

    public boolean isError() {
        return EVENT_ERROR.equals(event);
    }

    public boolean isPing() {
        return EVENT_PING.equals(event);
    }

    public boolean isDone() {
        return DATA_DONE.equals(data);
    }

    public boolean hasData() {
        return data != null && !data.isBlank();
    }
}
