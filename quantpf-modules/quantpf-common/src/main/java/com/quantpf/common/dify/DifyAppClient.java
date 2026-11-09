package com.quantpf.common.dify;

import reactor.core.publisher.Flux;

/**
 * Dify 应用客户端（Facade）。
 *
 * <p><strong>使用约束：本客户端只能在 Service 层调用</strong>，禁止在 Controller / Web 层直接使用——
 * 应由业务 Service 封装后再暴露接口（保证参数校验、鉴权、降级、审计等横切关注点在 Service 层统一处理）。</p>
 *
 * <p>调用方只需传入 {@link DifyAppType}（业务意图）与 {@link DifyRequest}（入参），客户端内部会：</p>
 * <ol>
 *     <li>按 {@code DifyAppType.code} 找到对应 Dify 应用的 apiKey；</li>
 *     <li>调用 {@code GET /v1/info}（带缓存）取得应用真实 {@code mode}；</li>
 *     <li>校验 mode 与业务意图的期望 mode 一致（不配对即抛 {@link DifyApiException}）；</li>
 *     <li>按真实 mode 路由到对应端点处理器，发起调用。</li>
 * </ol>
 *
 * <p>同步方法（{@link #invoke}）内部使用 {@code block()}，仅适合 Servlet / 独立线程等阻塞线程上调用；
 * 需要流式或组合异步场景请使用 {@link #stream} 返回的 {@link Flux}。</p>
 */
public interface DifyAppClient {

    /**
     * 查询并校验应用信息（带缓存），用于诊断或预检。
     *
     * @param appType 业务意图
     * @return 应用信息
     */
    DifyAppInfo getAppInfo(DifyAppType appType);

    /**
     * 流式调用，返回解析后的事件流（异步，不阻塞）。
     *
     * @param appType 业务意图
     * @param request 入参
     * @return 事件流
     */
    Flux<DifySseEvent> stream(DifyAppType appType, DifyRequest request);

    /**
     * 同步调用，阻塞等待并返回聚合结果。
     *
     * @param appType 业务意图
     * @param request 入参
     * @return 聚合结果
     */
    DifyResult invoke(DifyAppType appType, DifyRequest request);
}
