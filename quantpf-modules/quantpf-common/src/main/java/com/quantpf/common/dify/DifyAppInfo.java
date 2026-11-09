package com.quantpf.common.dify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code GET /v1/info} 返回的应用信息。
 *
 * <p>Dify 不同版本该接口返回字段略有差异，这里只取本客户端关心的字段，
 * 其余字段一律忽略（{@code ignoreUnknown}），保证向前兼容。</p>
 *
 * @param name        应用名称
 * @param description 应用描述
 * @param mode        应用类型 mode（chat / agent-chat / advanced-chat / completion / workflow）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DifyAppInfo(String name, String description, String mode) {
}
