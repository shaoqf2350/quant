package com.quantpf.common.dify;

import tools.jackson.databind.json.JsonMapper;
import io.netty.channel.ChannelOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.List;

/**
 * Dify 客户端自动装配。
 *
 * <p>通过 {@code quantpf.dify.enabled=true} 开启（默认关闭，未配置时避免产生无用 Bean）。
 * 装配共享的、带连接池的 {@link WebClient}，并注册三种端点处理器与门面客户端。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DifyProperties.class)
@ConditionalOnProperty(prefix = "quantpf.dify", name = "enabled", havingValue = "true")
public class DifyConfiguration {

    /**
     * 共享 WebClient：底层 Reactor Netty 连接池复用，避免每次调用新建连接（瓶颈点）。
     *
     * <p>注意：SSE 长连接不设置全局 responseTimeout，否则会掐断长流；
     * 同步调用方通过 {@code block(Duration)} 控制超时，流式调用由调用方自行控制。</p>
     */
    @Bean
    public WebClient difyWebClient(DifyProperties properties) {
        ConnectionProvider provider = ConnectionProvider.builder("dify")
            .maxConnections(properties.getMaxConnections())
            .pendingAcquireMaxCount(-1)
            .pendingAcquireTimeout(Duration.ofSeconds(45))
            .maxIdleTime(Duration.ofSeconds(20))
            .build();
        HttpClient httpClient = HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getConnectTimeout().toMillis())
            .compress(true);
        return WebClient.builder()
            .codecs(codecs -> {
                codecs.defaultCodecs().maxInMemorySize((int) properties.getMaxInMemorySize().toBytes());
                // 关键：WebClient 默认的 StringDecoder 会按 \n / \r\n 切行并剥离换行符，
                // 导致 SseEventParser 收不到空行（事件边界），把整条 Dify SSE 流误拼成一个事件。
                // 这里改用「保留换行符」的 StringDecoder，由 SseEventParser 自行按行切分。
                codecs.customCodecs().register(StringDecoder.allMimeTypes(List.of("\r\n", "\n"), false));
            })
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    @Bean
    public DifyChatMessageHandler difyChatMessageHandler(JsonMapper jsonMapper) {
        return new DifyChatMessageHandler(jsonMapper);
    }

    @Bean
    public DifyCompletionHandler difyCompletionHandler(JsonMapper jsonMapper) {
        return new DifyCompletionHandler(jsonMapper);
    }

    @Bean
    public DifyWorkflowHandler difyWorkflowHandler(JsonMapper jsonMapper) {
        return new DifyWorkflowHandler(jsonMapper);
    }

    @Bean
    public DifyAppClient difyAppClient(WebClient difyWebClient, DifyProperties properties, List<DifyEndpointHandler> handlers) {
        return new DefaultDifyAppClient(difyWebClient, properties, handlers);
    }
}
