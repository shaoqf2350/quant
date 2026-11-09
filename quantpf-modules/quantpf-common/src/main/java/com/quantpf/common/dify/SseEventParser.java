package com.quantpf.common.dify;

import java.util.ArrayList;
import java.util.List;

/**
 * SSE（Server-Sent Events）流式响应解析工具类。
 *
 * <p>职责：将 Dify 流式响应中的原始文本解析为结构化事件（{@link DifySseEvent}），
 * 尤其负责解析 SSE 的 {@code event:} 字段（事件类型）与 {@code data:} 字段（JSON 载荷）。</p>
 *
 * <p>支持两种使用方式：</p>
 * <ol>
 *     <li><strong>一次性解析</strong>：{@link #parse(String)}，适用于已拿到完整 SSE 文本的场景；</li>
 *     <li><strong>增量解析</strong>：{@link #newParser()} 返回的 {@link Parser} 可不断
 *     {@link Parser#push(String)} 喂入网络分片（事件可能被切在任意字节边界），配合
 *     {@link Parser#drain()} 取出已完整的事件，流结束时调用 {@link Parser#finish()} 冲刷剩余数据。
 *     适合 WebClient {@code bodyToFlux} 等流式场景。</li>
 * </ol>
 *
 * <p>遵循 SSE 规范：忽略 {@code :} 开头的注释行，处理 {@code event / data / id / retry} 字段，
 * 支持多行 {@code data:}（以 {@code \n} 拼接）、{@code \r\n} 换行、空行事件分界，
 * 事件字段可能被切在任意字符边界（增量模式下不丢字段）。</p>
 */
public final class SseEventParser {

    /** 未显式声明 event 字段时的默认事件类型（SSE 规范约定） */
    private static final String DEFAULT_EVENT = "message";

    private SseEventParser() {
    }

    /**
     * 一次性解析完整的 SSE 文本。
     *
     * @param raw SSE 原始文本
     * @return 解析出的全部事件（保持顺序）
     */
    public static List<DifySseEvent> parse(String raw) {
        Parser parser = new Parser();
        parser.push(raw);
        return parser.finish();
    }

    /**
     * 创建一个可增量解析的解析器（每个流实例一个，非线程安全）。
     */
    public static Parser newParser() {
        return new Parser();
    }

    /**
     * 有状态的增量 SSE 解析器。
     */
    public static final class Parser {

        /** 尚未遇到换行、可能不完整的行缓冲 */
        private final StringBuilder lineBuffer = new StringBuilder();

        /** 当前正在累积的事件的 data（多行 data 以 \n 拼接） */
        private final StringBuilder data = new StringBuilder();

        /** 当前事件的 event 类型 */
        private String eventName;

        /** 当前事件的 id */
        private String id;

        /** 当前事件的 retry */
        private Long retry;

        /** 当前事件是否已累积任意字段（避免空行/连续空行产生空事件） */
        private boolean seenField;

        /** 已解析完成、等待消费的事件 */
        private final List<DifySseEvent> ready = new ArrayList<>();

        private Parser() {
        }

        /**
         * 喂入一段网络分片，识别其中的完整行并累积事件。
         * 已完整的事件可从 {@link #drain()} 取出。
         *
         * @param chunk 一段原始文本（允许在任意位置被切断）
         */
        public void push(String chunk) {
            if (chunk == null || chunk.isEmpty()) {
                return;
            }
            lineBuffer.append(chunk);
            int idx;
            while ((idx = lineBuffer.indexOf("\n")) >= 0) {
                String line = lineBuffer.substring(0, idx);
                lineBuffer.delete(0, idx + 1);
                handleLine(line);
            }
        }

        /**
         * 取出并清空已解析完成的事件（通常在每次 push 后调用）。
         */
        public List<DifySseEvent> drain() {
            if (ready.isEmpty()) {
                return List.of();
            }
            List<DifySseEvent> out = new ArrayList<>(ready);
            ready.clear();
            return out;
        }

        /**
         * 流结束调用：冲刷残留的行缓冲与最后一个未分界的事件，并返回剩余事件。
         */
        public List<DifySseEvent> finish() {
            if (lineBuffer.length() > 0) {
                handleLine(lineBuffer.toString());
                lineBuffer.setLength(0);
            }
            dispatch();
            return drain();
        }

        private void handleLine(String line) {
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            // 空行：一个事件结束
            if (line.isEmpty()) {
                dispatch();
                return;
            }
            // 注释行：忽略
            if (line.charAt(0) == ':') {
                return;
            }
            int colon = line.indexOf(':');
            String field = colon < 0 ? line : line.substring(0, colon);
            String value = colon < 0 ? "" : line.substring(colon + 1);
            // 字段值若以单个空格开头需去除（SSE 规范）
            if (value.startsWith(" ")) {
                value = value.substring(1);
            }
            switch (field) {
                case "event" -> eventName = value;
                case "data" -> {
                    if (data.length() > 0) {
                        data.append('\n');
                    }
                    data.append(value);
                    seenField = true;
                }
                case "id" -> {
                    id = value;
                    seenField = true;
                }
                case "retry" -> {
                    try {
                        retry = Long.parseLong(value.trim());
                        seenField = true;
                    } catch (NumberFormatException ignored) {
                        // 非法 retry 忽略
                    }
                }
                default -> {
                    // 未知字段忽略（SSE 规范要求）
                }
            }
        }

        private void dispatch() {
            if (!seenField) {
                return;
            }
            ready.add(new DifySseEvent(
                eventName == null ? DEFAULT_EVENT : eventName,
                data.toString(),
                id,
                retry == null ? -1L : retry));
            // 重置当前事件状态
            eventName = null;
            data.setLength(0);
            id = null;
            retry = null;
            seenField = false;
        }
    }
}
