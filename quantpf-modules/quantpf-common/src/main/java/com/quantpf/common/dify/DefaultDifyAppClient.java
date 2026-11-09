package com.quantpf.common.dify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link DifyAppClient} 默认实现。
 *
 * <p>职责：</p>
 * <ol>
 *     <li>解析应用：按 {@link DifyAppType#getCode()} 取 apiKey，调用 {@code GET /v1/info}（带 TTL 缓存）取得真实 mode；</li>
 *     <li>配对校验：真实 mode 与 {@link DifyAppType#getExpectedMode()} 不一致时抛 {@link DifyApiException}（应用与 apiKey 不配对）；</li>
 *     <li>路由分发：按真实 mode 找到对应 {@link DifyEndpointHandler}，发起同步 / 流式调用。</li>
 * </ol>
 *
 * <p>瓶颈优化：{@code /v1/info} 结果按 {@link DifyProperties#getInfoCacheTtl()} 缓存，避免每次调用都打 info 接口；
 * 底层 {@link WebClient} 复用 Reactor Netty 连接池（见 {@link DifyConfiguration}）。</p>
 */
@Slf4j
public class DefaultDifyAppClient implements DifyAppClient {

    private final WebClient webClient;

    private final DifyProperties properties;

    /** mode → 端点处理器 注册表（启动时构建，只读） */
    private final Map<DifyAppMode, DifyEndpointHandler> handlers;

    /** /v1/info 结果缓存（按业务意图，TTL 过期后重建） */
    private final ConcurrentHashMap<DifyAppType, InfoCacheEntry> infoCache = new ConcurrentHashMap<>();

    public DefaultDifyAppClient(WebClient webClient, DifyProperties properties, List<DifyEndpointHandler> handlers) {
        this.webClient = webClient;
        this.properties = properties;
        this.handlers = buildHandlerRegistry(handlers);
    }

    @Override
    public DifyAppInfo getAppInfo(DifyAppType appType) {
        return resolveApp(appType).info();
    }

    @Override
    public Flux<DifySseEvent> stream(DifyAppType appType, DifyRequest request) {
        Objects.requireNonNull(request, "request 不能为空");
        return Flux.defer(() -> {
            AppContext ctx = resolveApp(appType);
            DifyEndpointHandler handler = handlerFor(ctx.mode());
            log.info("Dify 发起流式调用 appType={} mode={} endpoint={} apiKey={}",
                appType, ctx.mode(), handler.endpointPath(), maskKey(ctx.apiKey()));
            return handler.stream(webClient, ctx.baseUrl(), ctx.apiKey(), request);
        });
    }

    @Override
    public DifyResult invoke(DifyAppType appType, DifyRequest request) {
        Objects.requireNonNull(request, "request 不能为空");
        AppContext ctx = resolveApp(appType);
        DifyEndpointHandler handler = handlerFor(ctx.mode());
        log.info("Dify 发起同步调用 appType={} mode={} endpoint={} apiKey={}",
            appType, ctx.mode(), handler.endpointPath(), maskKey(ctx.apiKey()));
        DifyResult result = handler.invoke(webClient, ctx.baseUrl(), ctx.apiKey(), request)
            .map(r -> {
                r.setAppType(appType);
                r.setMode(ctx.mode());
                return r;
            })
            .block(properties.getReadTimeout());
        if (result == null) {
            throw new DifyApiException("Dify 同步调用超时，appType=" + appType + " readTimeout=" + properties.getReadTimeout());
        }
        log.info("Dify 同步调用完成 appType={} mode={} taskId={} answerLen={} outputsKeys={}",
            appType, ctx.mode(), result.getTaskId(),
            result.getAnswer() == null ? 0 : result.getAnswer().length(),
            result.getOutputs() == null ? 0 : result.getOutputs().size());
        return result;
    }

    /**
     * 解析应用上下文：校验配置 → 取 /v1/info（带缓存）→ 校验 mode 配对。
     */
    private AppContext resolveApp(DifyAppType appType) {
        Objects.requireNonNull(appType, "appType 不能为空");
        DifyProperties.App app = properties.getApps().get(appType.getCode());
        if (app == null || app.getApiKey() == null || app.getApiKey().isBlank()) {
            throw new DifyApiException("未配置 Dify 应用 apiKey，appType=" + appType.getCode()
                + "，请在 quantpf.dify.apps." + appType.getCode() + ".api-key 配置");
        }
        String baseUrl = properties.getBaseUrl();
        DifyAppInfo info = info(baseUrl, app.getApiKey(), appType);
        DifyAppMode actual = DifyAppMode.of(info.mode());
        if (actual != appType.getExpectedMode()) {
            throw new DifyApiException("Dify 应用与 apiKey 不配对：appType=" + appType
                + " 期望 mode=" + appType.getExpectedMode().mode() + "，实际 mode=" + info.mode());
        }
        log.info("Dify 应用配对校验通过 appType={} mode={} baseUrl={} apiKey={}",
            appType, actual, baseUrl, maskKey(app.getApiKey()));
        return new AppContext(baseUrl, app.getApiKey(), actual, info);
    }

    /**
     * 获取应用信息（带 TTL 缓存）：缓存命中走缓存，未命中实时请求并回填。
     */
    private DifyAppInfo info(String baseUrl, String apiKey, DifyAppType appType) {
        long now = System.currentTimeMillis();
        InfoCacheEntry entry = infoCache.get(appType);
        if (entry != null && entry.expireAt() > now) {
            log.debug("Dify /v1/info 命中缓存 appType={}", appType);
            return entry.info();
        }
        log.info("Dify /v1/info 缓存未命中，实时获取 appType={} baseUrl={} apiKey={}",
            appType, baseUrl, maskKey(apiKey));
        DifyAppInfo info = webClient.get()
            .uri(baseUrl + "/info")
            .header("Authorization", "Bearer " + apiKey)
            .retrieve()
            .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new DifyApiException("Dify /v1/info 请求失败 status=" + resp.statusCode().value() + " body=" + body,
                    resp.statusCode().value(), null, body)))
            .bodyToMono(DifyAppInfo.class)
            .block(properties.getReadTimeout());
        if (info == null || info.mode() == null || info.mode().isBlank()) {
            throw new DifyApiException("Dify /v1/info 未返回 mode 字段，无法校验应用配对，appType=" + appType);
        }
        log.info("Dify /v1/info 获取成功 appType={} name={} mode={}", appType, info.name(), info.mode());
        infoCache.put(appType, new InfoCacheEntry(info, now + properties.getInfoCacheTtl().toMillis()));
        return info;
    }

    private DifyEndpointHandler handlerFor(DifyAppMode mode) {
        DifyEndpointHandler handler = handlers.get(mode);
        if (handler == null) {
            throw new DifyApiException("未注册 Dify mode=" + mode + " 的端点处理器");
        }
        return handler;
    }

    /**
     * 由处理器列表构建 mode → 处理器 注册表，重复注册直接失败（启动期暴露配置错误）。
     */
    private static Map<DifyAppMode, DifyEndpointHandler> buildHandlerRegistry(List<DifyEndpointHandler> list) {
        Map<DifyAppMode, DifyEndpointHandler> map = new EnumMap<>(DifyAppMode.class);
        for (DifyEndpointHandler handler : list) {
            for (DifyAppMode mode : handler.supportedModes()) {
                DifyEndpointHandler prev = map.put(mode, handler);
                if (prev != null && prev != handler) {
                    throw new IllegalStateException("Dify mode " + mode + " 被多个处理器重复注册");
                }
            }
        }
        return map;
    }

    /** 脱敏 apiKey：只保留末 4 位，避免日志泄露敏感信息 */
    private static String maskKey(String key) {
        if (key == null || key.length() <= 4) {
            return "****";
        }
        return "****" + key.substring(key.length() - 4);
    }

    /** 已解析的应用上下文 */
    private record AppContext(String baseUrl, String apiKey, DifyAppMode mode, DifyAppInfo info) {
    }

    /** /v1/info 缓存条目 */
    private record InfoCacheEntry(DifyAppInfo info, long expireAt) {
    }
}
