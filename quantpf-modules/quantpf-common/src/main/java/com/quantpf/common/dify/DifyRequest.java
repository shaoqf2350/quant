package com.quantpf.common.dify;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Dify 调用请求（统一的请求模型，覆盖所有应用类型）。
 *
 * <p>字段按端点取用，未用到的字段由对应处理器忽略：</p>
 * <ul>
 *     <li>{@code inputs}：所有应用类型均使用（变量输入）；</li>
 *     <li>{@code query}：仅 chat / agent-chat / advanced-chat 使用（用户提问）；</li>
 *     <li>{@code conversationId}：仅 chat 类应用使用（多轮会话续聊，可选）；</li>
 *     <li>{@code user}：终端用户标识，用于 Dify 侧会话隔离与审计（可选，默认 quantpf）；</li>
 *     <li>{@code files}：附件（可选），元素为 Dify 文件对象 Map。</li>
 * </ul>
 */
@Getter
@Setter
@ToString
public class DifyRequest {

    /** 变量输入 */
    private Map<String, Object> inputs;

    /** 用户提问（仅 chat / agent-chat / advanced-chat） */
    private String query;

    /** 会话 ID（仅 chat 类，续聊时传入） */
    private String conversationId;

    /** 终端用户标识 */
    private String user;

    /** 附件列表，元素为 Dify 文件对象 */
    private List<Map<String, Object>> files;

    public DifyRequest() {
    }

    /** inputs 防空：未设置时返回空 Map，便于序列化 */
    public Map<String, Object> safeInputs() {
        return inputs == null ? Map.of() : inputs;
    }

    /** files 防空：未设置时返回空 List，便于序列化 */
    public List<Map<String, Object>> safeFiles() {
        return files == null ? List.of() : files;
    }

    /** 构造 chat 类请求（仅提问） */
    public static DifyRequest ofChat(String query) {
        DifyRequest r = new DifyRequest();
        r.setQuery(query);
        return r;
    }

    /** 构造 chat 类请求（提问 + 变量输入） */
    public static DifyRequest ofChat(String query, Map<String, Object> inputs) {
        DifyRequest r = new DifyRequest();
        r.setQuery(query);
        r.setInputs(inputs);
        return r;
    }

    /** 构造 workflow / completion 类请求（仅变量输入） */
    public static DifyRequest ofInputs(Map<String, Object> inputs) {
        DifyRequest r = new DifyRequest();
        r.setInputs(inputs);
        return r;
    }

    /** 构造 workflow / completion 类请求（变量输入 + 用户标识） */
    public static DifyRequest ofInputs(Map<String, Object> inputs, String user) {
        DifyRequest r = new DifyRequest();
        r.setInputs(inputs);
        r.setUser(user);
        return r;
    }
}
