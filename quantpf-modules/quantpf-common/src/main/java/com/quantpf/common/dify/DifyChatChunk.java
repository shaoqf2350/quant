package com.quantpf.common.dify;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 流式对话输出分片（用于 SSE 直出给前端）。
 *
 * <p>由 {@link DifyAppService} 把 Dify 的原始 SSE 事件聚合为可展示的分片：
 * 增量回答以 {@code answer} 分片逐段推送，流结束推 {@code done}，出错推 {@code error}。</p>
 *
 * @param type           分片类型：answer / done / error
 * @param content        内容（answer 为增量文本，error 为错误信息，done 为空）
 * @param conversationId 会话 ID（仅 done 分片回填，供多轮续聊使用）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DifyChatChunk(String type, String content, String conversationId) {

    public static final String TYPE_ANSWER = "answer";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    /** 增量回答分片 */
    public static DifyChatChunk answer(String delta) {
        return new DifyChatChunk(TYPE_ANSWER, delta, null);
    }

    /** 流结束分片（可携带 conversationId） */
    public static DifyChatChunk done(String conversationId) {
        return new DifyChatChunk(TYPE_DONE, null, conversationId);
    }

    /** 错误分片 */
    public static DifyChatChunk error(String message) {
        return new DifyChatChunk(TYPE_ERROR, message, null);
    }
}
