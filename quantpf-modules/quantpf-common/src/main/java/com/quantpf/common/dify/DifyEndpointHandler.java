package com.quantpf.common.dify;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dify Service API 端点处理器抽象基类（策略模式）。
 *
 * <p>一个子类负责一类端点（chat-messages / completion-messages / workflows/run），
 * 抽象出「构造请求体 → 发流式请求 → 解析 SSE → 聚合结果」的模板流程；
 * 子类只需实现 {@link #supportedModes()}、{@link #endpointPath()}、{@link #buildBody(DifyRequest)}
 * 与 {@link #aggregate(Flux)} 四个差异点，公共的 HTTP 调用、SSE 解析、错误处理均在本类复用。</p>
 */
@Slf4j
public abstract class DifyEndpointHandler {

    protected static final MediaType APPLICATION_JSON = MediaType.APPLICATION_JSON;

    protected static final String HEADER_AUTHORIZATION = "Authorization";

    /** Dify 流式响应模式（本客户端统一采用 streaming 后聚合，避免维护 blocking 两套解析） */
    protected static final String RESPONSE_MODE_STREAMING = "streaming";

    protected final JsonMapper objectMapper;

    protected DifyEndpointHandler(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 该处理器负责的 Dify mode 集合。
     */
    public abstract Set<DifyAppMode> supportedModes();

    /**
     * 相对 baseUrl 的端点路径（如 /chat-messages）。
     */
    public abstract String endpointPath();

    /**
     * 构造请求体（response_mode 由基类统一补充为 streaming，子类无需关心）。
     */
    protected abstract Map<String, Object> buildBody(DifyRequest request);

    /**
     * 将流式事件聚合为最终结果。
     */
    protected abstract Mono<DifyResult> aggregate(Flux<DifySseEvent> events);

    /**
     * 发起流式调用，返回解析后的事件流（异步，不阻塞）。
     *
     * <p>关键点：为每个订阅创建独立的 {@link SseEventParser.Parser}，用 {@code Flux.defer} 保证
     * 多次订阅（重试/重放）时解析器状态互不污染；底层依赖 {@code text/event-stream} 的分块投递，
     * 而解析器本身容忍任意字节边界切分，两者叠加保证不丢事件。</p>
     */
    public Flux<DifySseEvent> stream(WebClient client, String baseUrl, String apiKey, DifyRequest request) {
        return Flux.defer(() -> {
            SseEventParser.Parser parser = SseEventParser.newParser();
            return client.post()
                .uri(baseUrl + endpointPath())
                .header(HEADER_AUTHORIZATION, bearer(apiKey))
                .contentType(APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(buildBody(request))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> toApiException(resp.statusCode().value(), body)))
                .bodyToFlux(String.class)
                .concatMap(chunk -> {
                    parser.push(chunk);
                    return Flux.fromIterable(parser.drain());
                })
                .concatWith(Flux.defer(() -> Flux.fromIterable(parser.finish())))
                .doOnNext(this::logEvent);
        });
    }

    /**
     * 发起同步调用（流式 + 聚合），返回 Mono，由上层决定 block 时机。
     */
    public Mono<DifyResult> invoke(WebClient client, String baseUrl, String apiKey, DifyRequest request) {
        return aggregate(stream(client, baseUrl, apiKey, request));
    }

    /**
     * 通用聚合：拼装 chat / completion 类的 message 事件 answer，
     * 并从 message_end 提取 task_id / conversation_id；遇到 error 事件时抛异常。
     */
    protected Mono<DifyResult> aggregateMessageStream(Flux<DifySseEvent> events) {
        AtomicReference<String> errorRef = new AtomicReference<>();
        return events
            .reduceWith(DifyResult::new, (acc, e) -> {
                String evt = eventType(e);
                if (DifySseEvent.EVENT_AGENT_MESSAGE.equals(evt)) {
                    // Agent 应用（agent-chat）：answer 为增量文本，逐段累加
                    String answer = textOf(readJson(e.data()), "answer");
                    if (answer != null) {
                        acc.setAnswer(acc.getAnswer() == null ? answer : acc.getAnswer() + answer);
                    }
                } else if (DifySseEvent.EVENT_MESSAGE.equals(evt)) {
                    // 普通 chat 应用：answer 为累计全文，直接覆盖（避免重复拼接）
                    String answer = textOf(readJson(e.data()), "answer");
                    if (answer != null) {
                        acc.setAnswer(answer);
                    }
                } else if (DifySseEvent.EVENT_MESSAGE_END.equals(evt)) {
                    JsonNode node = readJson(e.data());
                    acc.setTaskId(textOf(node, "task_id"));
                    acc.setConversationId(textOf(node, "conversation_id"));
                } else if (DifySseEvent.EVENT_ERROR.equals(evt)) {
                    JsonNode node = readJson(e.data());
                    String msg = textOf(node, "message");
                    errorRef.set(msg != null ? msg : e.data());
                }
                return acc;
            })
            .map(acc -> {
                if (errorRef.get() != null) {
                    throw new DifyApiException("Dify 流式返回 error 事件: " + errorRef.get());
                }
                return acc;
            });
    }

    /**
     * 解析 JSON，失败返回 nullNode（并告警），保证流式聚合不被单个坏事件中断。
     */
    protected JsonNode readJson(String data) {
        if (data == null || data.isBlank()) {
            return objectMapper.getNodeFactory().nullNode();
        }
        try {
            return objectMapper.readTree(data);
        } catch (Exception ex) {
            log.warn("解析 Dify SSE data 失败，已忽略该事件: {}", data, ex);
            return objectMapper.getNodeFactory().nullNode();
        }
    }

    /**
     * 取 JSON 对象文本字段；缺失 / 为 null 时返回 null。
     */
    protected String textOf(JsonNode node, String field) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    /**
     * 解析 Dify 事件真实类型。
     *
     * <p>Dify 的 SSE 流并不写 {@code event:} 字段，而是把事件类型放在 {@code data} JSON 的
     * {@code "event"} 字段里（如 {@code data: {"event":"agent_message",...}}）。因此优先取 JSON 里的
     * {@code event}，缺失时回退到 SSE 的 {@code event} 字段（兼容标准 SSE）。</p>
     */
    protected String eventType(DifySseEvent e) {
        JsonNode node = readJson(e.data());
        JsonNode event = node.get("event");
        if (event != null && !event.isNull() && !event.asText().isBlank()) {
            return event.asText();
        }
        return e.event();
    }

    /** 终端用户标识兜底（Dify 要求非空） */
    protected static String defaultUser(String user) {
        return (user == null || user.isBlank()) ? "quantpf" : user;
    }

    protected static String bearer(String apiKey) {
        return "Bearer " + apiKey;
    }

    private void logEvent(DifySseEvent e) {
        String event = eventType(e);
        if (DifySseEvent.EVENT_ERROR.equals(event)) {
            log.warn("Dify SSE 返回 error 事件: {}", e.data());
        } else if (DifySseEvent.EVENT_PING.equals(event)) {
            log.trace("Dify SSE ping（心跳）");
        } else {
            log.debug("Dify SSE 事件 event={} dataLen={}", event, e.data() == null ? 0 : e.data().length());
        }
    }

    private DifyApiException toApiException(int status, String body) {
        JsonNode node = readJson(body);
        String message = textOf(node, "message");
        String code = textOf(node, "code");
        if (message == null && body != null && !body.isBlank()) {
            message = body;
        }
        return new DifyApiException("Dify HTTP 请求失败 status=" + status + (message != null ? " message=" + message : ""),
            status, code, body);
    }
}
