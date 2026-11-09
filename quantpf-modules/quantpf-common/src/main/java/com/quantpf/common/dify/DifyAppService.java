package com.quantpf.common.dify;

import reactor.core.publisher.Flux;

/**
 * Dify 应用服务（业务门面，供 Controller / 其他 Service 调用）。
 *
 * <p>本服务是 {@link DifyAppClient} 的唯一合法调用方：它把「智能体对话」这一业务场景
 * 封装为流式与同步两个入口，隐藏 Dify 事件细节，向上输出可直接展示的 {@link DifyChatChunk}。</p>
 *
 * <p>当前示例固定使用 {@link DifyAppType#AGENT}（agent-chat）应用；如后续需要支持多种业务意图，
 * 可将 {@link DifyAppType} 作为入参下放。</p>
 */
public interface DifyAppService {

    /**
     * 流式对话：返回聚合后的答案分片流（异步，不阻塞），适合 SSE 直出前端。
     *
     * @param request 对话请求（query 必填，conversationId 可选用于多轮续聊）
     * @return 答案分片流，末尾以 {@code done} 分片结束；出错时以 onError 结束
     */
    Flux<DifyChatChunk> streamChat(DifyRequest request);

    /**
     * 同步对话：阻塞等待完整回答（最小可跑的调用示例，内部使用 {@link DifyAppClient#invoke}）。
     *
     * @param request 对话请求
     * @return 完整回答文本
     */
    String chat(DifyRequest request);
}
