package com.quantpf.common.dify;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link DifyAppService} 默认实现。
 *
 * <p>演示「Service 层调用 {@link DifyAppClient}」的最小可跑示例：
 * 流式场景把 Dify 事件聚合为 {@link DifyChatChunk}（agent-chat 的 {@code agent_message} 增量、chat 的
 * {@code message} 累计全文均正确处理），同步场景直接使用 {@link DifyAppClient#invoke}。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "quantpf.dify", name = "enabled", havingValue = "true")
public class DifyAppServiceImpl implements DifyAppService {

    /** 演示用终端用户标识（Dify 要求非空） */
    private static final String DEMO_USER = "quantpf-demo";

    private final DifyAppClient difyAppClient;
    private final JsonMapper objectMapper;

    public DifyAppServiceImpl(DifyAppClient difyAppClient, JsonMapper objectMapper) {
        this.difyAppClient = difyAppClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<DifyChatChunk> streamChat(DifyRequest request) {
        DifyRequest effective = normalize(request);
        return Flux.defer(() -> {
            // 每个订阅独立的状态：累计全文已打印长度 / 会话 ID
            AtomicInteger printed = new AtomicInteger();
            AtomicReference<String> conversationRef = new AtomicReference<>(effective.getConversationId());
            log.info("Dify 流式对话开始 queryLen={} conversationId={}",
                effective.getQuery().length(), effective.getConversationId());
            return difyAppClient.stream(DifyAppType.AGENT, effective)
                .concatMap(evt -> toChunk(evt, printed, conversationRef))
                .concatWith(Flux.defer(() -> Flux.just(DifyChatChunk.done(conversationRef.get()))))
                .doOnComplete(() -> log.info("Dify 流式对话完成 conversationId={}", conversationRef.get()));
        });
    }

    @Override
    public String chat(DifyRequest request) {
        DifyRequest effective = normalize(request);
        log.info("Dify 同步对话开始 queryLen={}", effective.getQuery().length());
        DifyResult result = difyAppClient.invoke(DifyAppType.AGENT, effective);
        log.info("Dify 同步对话完成 answerLen={}", result.getAnswer() == null ? 0 : result.getAnswer().length());
        return result.getAnswer();
    }

    /**
     * 把单条 Dify SSE 事件映射为 0..1 个答案分片。
     */
    private Flux<DifyChatChunk> toChunk(DifySseEvent evt, AtomicInteger printed, AtomicReference<String> conversationRef) {
        String name = eventType(evt);
        if (DifySseEvent.EVENT_ERROR.equals(name)) {
            String message = text(evt.data(), "message");
            return Flux.error(new DifyApiException(message != null ? message : "Dify 流式返回 error 事件"));
        }
        if (DifySseEvent.EVENT_AGENT_MESSAGE.equals(name)) {
            // agent-chat：answer 为增量文本，直接作为一段回答
            String answer = text(evt.data(), "answer");
            return (answer == null || answer.isEmpty()) ? Flux.empty() : Flux.just(DifyChatChunk.answer(answer));
        }
        if (DifySseEvent.EVENT_MESSAGE.equals(name)) {
            // chat：answer 为累计全文，只截取新增部分作为增量
            String answer = text(evt.data(), "answer");
            if (answer == null) {
                return Flux.empty();
            }
            int prev = printed.get();
            if (answer.length() > prev) {
                printed.set(answer.length());
                return Flux.just(DifyChatChunk.answer(answer.substring(prev)));
            }
            return Flux.empty();
        }
        if (DifySseEvent.EVENT_MESSAGE_END.equals(name)) {
            String cid = text(evt.data(), "conversation_id");
            if (cid != null && !cid.isBlank()) {
                conversationRef.set(cid);
            }
            return Flux.empty();
        }
        // agent_thought / ping / message_replace 等事件不产生展示文本
        return Flux.empty();
    }

    /**
     * 校验并补齐请求：query 必填、user 默认。
     */
    private DifyRequest normalize(DifyRequest request) {
        if (request == null) {
            throw new DifyApiException("Dify 对话请求不能为空");
        }
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            throw new DifyApiException("Dify 对话请求缺少 query");
        }
        if (request.getUser() == null || request.getUser().isBlank()) {
            request.setUser(DEMO_USER);
        }
        return request;
    }

    /**
     * 取 JSON 载荷中的文本字段；载荷非法时忽略并告警。
     */
    private String text(String data, String field) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode value = node.get(field);
            return (value == null || value.isNull()) ? null : value.asText();
        } catch (Exception ex) {
            log.warn("解析 Dify SSE data 失败，已忽略: {}", data, ex);
            return null;
        }
    }

    /**
     * 解析 Dify 事件真实类型：Dify 把事件类型放在 data JSON 的 {@code "event"} 字段里，
     * 而非 SSE 的 {@code event:} 字段；缺失时回退到 SSE 事件字段。
     */
    private String eventType(DifySseEvent evt) {
        String fromData = text(evt.data(), "event");
        return (fromData != null && !fromData.isBlank()) ? fromData : evt.event();
    }
}
