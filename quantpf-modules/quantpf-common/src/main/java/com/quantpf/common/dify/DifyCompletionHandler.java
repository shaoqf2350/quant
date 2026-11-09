package com.quantpf.common.dify;

import tools.jackson.databind.json.JsonMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * completion-messages 端点处理器：负责 completion（文本生成）类应用。
 *
 * <p>走 {@code POST /completion-messages}，请求体仅 inputs / user / files，
 * 流式事件与 chat 类一致（message 拼 answer、message_end 结束），故复用父类聚合。</p>
 */
public class DifyCompletionHandler extends DifyEndpointHandler {

    public DifyCompletionHandler(JsonMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Set<DifyAppMode> supportedModes() {
        return Set.of(DifyAppMode.COMPLETION);
    }

    @Override
    public String endpointPath() {
        return "/completion-messages";
    }

    @Override
    protected Map<String, Object> buildBody(DifyRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", request.safeInputs());
        body.put("response_mode", RESPONSE_MODE_STREAMING);
        body.put("user", defaultUser(request.getUser()));
        body.put("files", request.safeFiles());
        return body;
    }

    @Override
    protected Mono<DifyResult> aggregate(Flux<DifySseEvent> events) {
        return aggregateMessageStream(events);
    }
}
