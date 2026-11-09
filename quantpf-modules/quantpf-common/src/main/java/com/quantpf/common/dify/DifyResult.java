package com.quantpf.common.dify;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Dify 调用结果（同步 {@link DifyAppClient#invoke} 返回）。
 *
 * <p>不同应用类型填充不同字段：chat/completion 填充 {@link #answer}，workflow 填充 {@link #outputs}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifyResult {

    /** 业务意图（由调用方传入，客户端回填） */
    private DifyAppType appType;

    /** 实际 mode（由 /v1/info 决定，客户端回填） */
    private DifyAppMode mode;

    /** chat/completion 聚合出的完整回答文本 */
    private String answer;

    /** workflow 运行结束的输出变量 */
    private Map<String, Object> outputs;

    /** 本次运行的任务 ID（task_id） */
    private String taskId;

    /** 会话 ID（chat 类，来自 message_end，供续聊使用） */
    private String conversationId;
}
