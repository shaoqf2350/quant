package com.quantpf.common.dify;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * workflows/run 端点处理器：负责 workflow（工作流）类应用。
 *
 * <p>走 {@code POST /workflows/run}，关注 {@code workflow_finished} 事件：
 * 校验运行状态为 succeeded、取出 {@code data.outputs} 作为最终输出；
 * 未收到 workflow_finished 或返回 error 事件时抛异常。</p>
 */
public class DifyWorkflowHandler extends DifyEndpointHandler {

    public DifyWorkflowHandler(JsonMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Set<DifyAppMode> supportedModes() {
        return Set.of(DifyAppMode.WORKFLOW);
    }

    @Override
    public String endpointPath() {
        return "/workflows/run";
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
        AtomicReference<String> taskId = new AtomicReference<>();
        AtomicReference<JsonNode> finished = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        return events
            .doOnNext(e -> {
                String event = eventType(e);
                if (DifySseEvent.EVENT_ERROR.equals(event)) {
                    error.set(e.data());
                } else if (DifySseEvent.EVENT_WORKFLOW_FINISHED.equals(event)) {
                    JsonNode root = readJson(e.data());
                    finished.set(root);
                    taskId.set(textOf(root, "task_id"));
                }
            })
            .then(Mono.defer(() -> {
                if (error.get() != null) {
                    return Mono.error(new DifyApiException("Dify 工作流返回 error 事件: " + error.get()));
                }
                JsonNode root = finished.get();
                if (root == null || root.isNull() || root.isMissingNode()) {
                    return Mono.error(new DifyApiException("Dify 工作流未返回 workflow_finished 事件，无法取得输出"));
                }
                JsonNode data = root.path("data");
                String status = data.path("status").asText("");
                JsonNode err = data.path("error");
                if (err != null && !err.isNull() && !err.asText().isBlank()) {
                    return Mono.error(new DifyApiException("Dify 工作流执行失败: " + err.asText()));
                }
                if (!"succeeded".equals(status)) {
                    return Mono.error(new DifyApiException("Dify 工作流状态异常: " + status));
                }
                Map<String, Object> outputs = objectMapper.convertValue(
                    data.path("outputs"), new TypeReference<Map<String, Object>>() { });
                return Mono.just(DifyResult.builder().outputs(outputs).taskId(taskId.get()).build());
            }));
    }
}
