package com.quantpf.web.controller;

import com.quantpf.common.dify.DifyApiException;
import com.quantpf.common.dify.DifyAppService;
import com.quantpf.common.dify.DifyChatChunk;
import com.quantpf.common.dify.DifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

/**
 * Dify AI 对话接口（《Dify 客户端落地方案》最小可跑示例）。
 *
 * <p>流式对话以 SSE（Server-Sent Events）返回：{@code answer} 分片为增量回答，{@code done} 为流结束，
 * {@code error} 为异常。请求需登录（Sa-Token），前端需带 {@code Authorization} 与 {@code clientid} 头。</p>
 */
@Slf4j
@Tag(name = "Dify AI 对话", description = "基于 Dify 智能体（agent-chat）的流式对话")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/quant/dify")
public class DifyAppController {

    /** SSE 连接超时（毫秒），覆盖 Dify 长流 */
    private static final long SSE_TIMEOUT_MS = 300_000L;

    private final DifyAppService difyAppService;

    /**
     * 流式对话（SSE）。请求体：{@code {"query":"你好","conversationId":"..."}}。
     */
    @Operation(summary = "流式对话（SSE）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody DifyRequest request, HttpServletResponse response) {
        prepareSseResponse(response);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Disposable subscription = difyAppService.streamChat(request)
            .subscribe(
                chunk -> send(emitter, chunk),
                error -> sendErrorAndComplete(emitter, error),
                emitter::complete
            );
        // 客户端断开 / 超时 / 异常时取消上游订阅，避免资源泄漏
        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(subscription::dispose);
        emitter.onError(e -> subscription.dispose());
        return emitter;
    }

    /**
     * 同步对话（非流式，最小可跑示例）。请求体：{@code {"query":"你好"}}。
     */
    @Operation(summary = "同步对话")
    @PostMapping("/chat")
    public R<String> chat(@RequestBody DifyRequest request) {
        return R.ok(difyAppService.chat(request));
    }

    private void send(SseEmitter emitter, DifyChatChunk chunk) {
        try {
            emitter.send(chunk);
        } catch (Exception e) {
            // 客户端可能已断开
            emitter.completeWithError(e);
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, Throwable error) {
        String message = error instanceof DifyApiException ? error.getMessage() : "AI 对话失败：" + error.getMessage();
        log.warn("Dify 流式对话异常: {}", message);
        try {
            emitter.send(DifyChatChunk.error(message));
        } catch (Exception ignored) {
            // 客户端已断开，忽略
        } finally {
            emitter.complete();
        }
    }

    private void prepareSseResponse(HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
    }
}
