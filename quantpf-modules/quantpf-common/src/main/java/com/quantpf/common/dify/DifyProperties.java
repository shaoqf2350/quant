package com.quantpf.common.dify;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dify 客户端配置（前缀 {@code quantpf.dify}）。
 *
 * <p>示例（配置在环境文件 application.yml 中，敏感值用环境变量占位）：</p>
 * <pre>
 * quantpf:
 *   dify:
 *     enabled: true
 *     base-url: ${DIFY_BASE_URL:http://localhost/v1}
 *     apps:
 *       chat:
 *         api-key: ${DIFY_CHAT_API_KEY:}
 *       agent:
 *         api-key: ${DIFY_AGENT_API_KEY:}
 *       # ... workflow / chatflow / text-generation 同理
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "quantpf.dify")
public class DifyProperties {

    /** 总开关（默认关闭，未启用时不装配任何 Dify Bean） */
    private boolean enabled = false;

    /** Dify Service API 根地址（含 /v1），如 http://localhost/v1 */
    private String baseUrl = "http://localhost/v1";

    /** 建立连接超时 */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /** 同步调用读超时（SSE 长流不适用此值，流式由调用方自行控制） */
    private Duration readTimeout = Duration.ofSeconds(300);

    /** /v1/info 结果缓存时长（避免每次调用都请求 info 接口，瓶颈优化） */
    private Duration infoCacheTtl = Duration.ofSeconds(300);

    /** 连接池最大连接数 */
    private int maxConnections = 200;

    /** WebClient 编解码单块内存上限 */
    private DataSize maxInMemorySize = DataSize.ofMegabytes(10);

    /** 各业务意图对应的 Dify 应用配置，key 为 {@link DifyAppType#getCode()} */
    private Map<String, App> apps = new LinkedHashMap<>();

    /**
     * 单个 Dify 应用配置。
     */
    @Data
    public static class App {

        /** 应用 API Key（与 /v1/info 校验到的应用一一对应） */
        private String apiKey;
    }
}
