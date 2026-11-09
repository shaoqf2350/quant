package com.quantpf.common.dify;

import lombok.Getter;

import java.io.Serial;

/**
 * Dify 调用相关异常（配置缺失、配对校验失败、HTTP 非 2xx、流式返回 error 事件等）。
 *
 * <p>继承 {@link RuntimeException}，可被上层 Service 捕获并做降级
 * （例如研究任务在适配器异常时转人工复核，而不是直接判失败）。</p>
 */
@Getter
public class DifyApiException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** HTTP 状态码（无 HTTP 语义时为 null） */
    private final Integer httpStatus;

    /** Dify 返回的错误码（如 invalid_api_key） */
    private final String errorCode;

    /** 详细错误信息（HTTP 响应体原文 / SSE error 事件 data） */
    private final String detailMessage;

    public DifyApiException(String message) {
        this(message, null, null, null);
    }

    public DifyApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = null;
        this.errorCode = null;
        this.detailMessage = null;
    }

    public DifyApiException(String message, Integer httpStatus, String errorCode, String detailMessage) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }
}
