import { getToken } from '@/utils/auth';

/**
 * Dify AI 对话 API（流式 SSE）。
 *
 * 说明：SSE 无法用 axios 的 JSON 封装优雅消费，这里用原生 fetch + ReadableStream 解析事件流。
 * 后端以 SSE 返回：answer 分片（增量回答）/ done（结束，可带 conversationId）/ error（异常）。
 */

export interface DifyChatChunk {
  type: 'answer' | 'done' | 'error';
  content?: string;
  conversationId?: string;
}

export interface StreamHandlers {
  onAnswer?: (delta: string) => void;
  onDone?: (conversationId?: string) => void;
  onError?: (message: string) => void;
}

/**
 * 发起流式对话。
 *
 * @param query          用户提问
 * @param conversationId 会话 ID（多轮续聊，可选）
 * @param handlers       分片回调
 * @param signal         取消信号（关闭对话框时中止）
 */
export async function streamDifyChat(
  query: string,
  conversationId: string | undefined,
  handlers: StreamHandlers,
  signal?: AbortSignal
): Promise<void> {
  const resp = await fetch(`${import.meta.env.VITE_APP_BASE_API}/api/quant/dify/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + getToken(),
      clientid: import.meta.env.VITE_APP_CLIENT_ID
    },
    body: JSON.stringify({ query, conversationId }),
    signal
  });

  if (!resp.ok) {
    let message = `请求失败：HTTP ${resp.status}`;
    try {
      const data = await resp.json();
      message = data.msg || data.message || message;
    } catch {
      // 非 JSON 错误体，保留默认提示
    }
    handlers.onError?.(message);
    return;
  }

  if (!resp.body) {
    handlers.onError?.('当前浏览器不支持流式响应');
    return;
  }

  const reader = resp.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      let idx: number;
      while ((idx = buffer.indexOf('\n\n')) !== -1) {
        const raw = buffer.slice(0, idx);
        buffer = buffer.slice(idx + 2);
        handleSseEvent(raw, handlers);
      }
    }
    // 处理末尾可能没有空行结尾的事件
    if (buffer.trim()) {
      handleSseEvent(buffer, handlers);
    }
  } finally {
    reader.releaseLock();
  }
}

function handleSseEvent(raw: string, handlers: StreamHandlers) {
  let data = '';
  for (const line of raw.split('\n')) {
    if (line.startsWith('data:')) {
      data += line.slice(5).replace(/^\s/, '');
      data += '\n';
    }
  }
  data = data.trim();
  if (!data) return;

  let chunk: DifyChatChunk;
  try {
    chunk = JSON.parse(data);
  } catch {
    return;
  }

  if (chunk.type === 'answer' && chunk.content) {
    handlers.onAnswer?.(chunk.content);
  } else if (chunk.type === 'done') {
    handlers.onDone?.(chunk.conversationId);
  } else if (chunk.type === 'error') {
    handlers.onError?.(chunk.content || 'AI 对话出错');
  }
}
