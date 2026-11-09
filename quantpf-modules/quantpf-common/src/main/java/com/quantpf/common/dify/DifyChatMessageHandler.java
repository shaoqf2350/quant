package com.quantpf.common.dify;

import tools.jackson.databind.json.JsonMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * chat-messages 端点处理器：负责 chat / agent-chat / advanced-chat 三类应用。
 *
 * <p>三者均走 {@code POST /chat-messages}，差异仅在于 Dify 应用内部的编排方式，请求体与解析方式一致。</p>
 */
public class DifyChatMessageHandler extends DifyEndpointHandler {

    public DifyChatMessageHandler(JsonMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Set<DifyAppMode> supportedModes() {
        return Set.of(DifyAppMode.CHAT, DifyAppMode.AGENT_CHAT, DifyAppMode.ADVANCED_CHAT);
    }

    @Override
    public String endpointPath() {
        return "/chat-messages";
    }

    @Override
    protected Map<String, Object> buildBody(DifyRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", request.safeInputs());
        body.put("query", request.getQuery());
        body.put("response_mode", RESPONSE_MODE_STREAMING);
        if (request.getConversationId() != null && !request.getConversationId().isBlank()) {
            body.put("conversation_id", request.getConversationId());
        }
        body.put("user", defaultUser(request.getUser()));
        body.put("files", request.safeFiles());
        return body;
    }

    @Override
    protected Mono<DifyResult> aggregate(Flux<DifySseEvent> events) {
        return aggregateMessageStream(events);
    }
}
