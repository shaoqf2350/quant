<template>
  <Teleport to="body">
    <div v-if="modelValue" class="ai-chat-dialog">
      <div class="ai-chat-dialog__header">
        <span class="ai-chat-dialog__title">AI 会话</span>
        <button type="button" class="ai-chat-dialog__close" aria-label="关闭" @click="close">×</button>
      </div>

      <div ref="bodyRef" class="ai-chat-dialog__body">
        <div v-if="messages.length === 0" class="ai-chat-dialog__hint">你好，我是 AI 助手，有什么可以帮你？</div>
        <div v-for="(msg, i) in messages" :key="i" class="ai-chat-dialog__msg" :class="`is-${msg.role}`">
          <div class="ai-chat-dialog__bubble">{{ msg.content }}</div>
        </div>
        <div v-if="streaming" class="ai-chat-dialog__msg is-assistant">
          <div class="ai-chat-dialog__bubble ai-chat-dialog__bubble--typing">…</div>
        </div>
      </div>

      <div class="ai-chat-dialog__footer">
        <el-input
          v-model="input"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入问题，Enter 发送 / Shift+Enter 换行"
          :disabled="streaming"
          @keydown.enter.exact.prevent="send()"
        />
        <el-button type="primary" :disabled="streaming || !input.trim()" @click="send()">发送</el-button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import { streamDifyChat } from '@/api/quant/dify';

defineOptions({ name: 'AiChatDialog' });

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

const props = defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>();

const messages = ref<ChatMessage[]>([]);
const input = ref('');
const streaming = ref(false);
const bodyRef = ref<HTMLElement>();
const conversationId = ref<string | undefined>(undefined);
let abortController: AbortController | undefined;

// 首次打开自动提问「你好」
watch(
  () => props.modelValue,
  visible => {
    if (visible) {
      nextTick(() => {
        if (messages.value.length === 0) {
          send('你好');
        }
      });
    }
  }
);

function close() {
  abortController?.abort();
  emit('update:modelValue', false);
}

async function send(text?: string) {
  const query = (text ?? input.value).trim();
  if (!query || streaming.value) return;
  input.value = '';

  messages.value.push({ role: 'user', content: query });
  messages.value.push({ role: 'assistant', content: '' });
  const idx = messages.value.length - 1;

  streaming.value = true;
  abortController = new AbortController();
  try {
    await streamDifyChat(
      query,
      conversationId.value,
      {
        onAnswer: delta => {
          messages.value[idx].content += delta;
          scrollToBottom();
        },
        onDone: cid => {
          if (cid) conversationId.value = cid;
        },
        onError: message => {
          if (!messages.value[idx].content) messages.value[idx].content = message;
        }
      },
      abortController.signal
    );
  } catch {
    if (!messages.value[idx].content) messages.value[idx].content = '对话已中断';
  } finally {
    streaming.value = false;
    scrollToBottom();
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (bodyRef.value) {
      bodyRef.value.scrollTop = bodyRef.value.scrollHeight;
    }
  });
}
</script>

<style scoped lang="scss">
.ai-chat-dialog {
  position: fixed;
  right: 20px;
  bottom: 92px;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  width: 380px;
  max-width: calc(100vw - 32px);
  height: 520px;
  max-height: calc(100vh - 120px);
  overflow: hidden;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.16);

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  &__title {
    font-size: 15px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  &__close {
    width: 28px;
    height: 28px;
    border: 0;
    background: transparent;
    color: var(--el-text-color-secondary);
    font-size: 22px;
    line-height: 1;
    cursor: pointer;
    border-radius: 6px;

    &:hover {
      background: var(--el-fill-color-light);
      color: var(--el-text-color-primary);
    }
  }

  &__body {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__hint {
    margin: auto;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  &__msg {
    display: flex;

    &.is-user {
      justify-content: flex-end;
    }

    &.is-assistant {
      justify-content: flex-start;
    }
  }

  &__bubble {
    max-width: 82%;
    padding: 10px 14px;
    border-radius: 12px;
    font-size: 14px;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;

    .is-user & {
      background: var(--el-color-primary);
      color: #fff;
      border-bottom-right-radius: 4px;
    }

    .is-assistant & {
      background: var(--el-fill-color);
      color: var(--el-text-color-primary);
      border-bottom-left-radius: 4px;
    }

    &--typing {
      opacity: 0.6;
    }
  }

  &__footer {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid var(--el-border-color-lighter);

    .el-textarea {
      flex: 1;
    }
  }
}
</style>
