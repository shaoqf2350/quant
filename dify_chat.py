#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
dify_chat.py —— 命令行与本地 Dify（Agent Chat App）进行流式对话的脚本。

用法示例：
    python dify_chat.py "你好，介绍一下你自己"
    python dify_chat.py "你好" --user myname
    python dify_chat.py "继续上一轮" --conversation-id <上一个 conversation_id>
    echo "今天天气如何" | python dify_chat.py
"""

import argparse
import json
import sys

import requests

# ============ 可修改的配置 ============
DIFY_BASE_URL = "http://localhost:3000"
DIFY_API_KEY = "app-dgqpip6O77BlkGGhGoDKL3Q8"
DIFY_USER = "dify-cli-user"   # 默认用户标识，可按需修改
# =====================================

CHAT_MESSAGES_URL = f"{DIFY_BASE_URL}/v1/chat-messages"


def stream_chat(query, user=DIFY_USER, conversation_id=""):
    """调用 Dify 流式接口，逐字打印回答，返回最终的 conversation_id。"""
    headers = {
        "Authorization": f"Bearer {DIFY_API_KEY}",
        "Content-Type": "application/json",
    }
    payload = {
        "inputs": {},
        "query": query,
        "response_mode": "streaming",
        "conversation_id": conversation_id,
        "user": user,
    }

    with requests.post(
        CHAT_MESSAGES_URL,
        headers=headers,
        json=payload,
        stream=True,
        timeout=(10, None),   # 连接超时 10s，读取不超时
    ) as resp:
        if resp.status_code != 200:
            print(f"[错误] HTTP {resp.status_code}: {resp.text}", file=sys.stderr)
            return conversation_id

        # SSE 内容统一按 UTF-8 解码，避免中文乱码
        resp.encoding = "utf-8"

        # 普通 chat 应用的 message 事件，answer 是“累计全文”，
        # 这里记录已打印长度，只输出新增部分。
        printed = 0

        for line in resp.iter_lines(decode_unicode=True):
            if not line:
                continue
            line = line.strip()
            if not line.startswith("data:"):
                continue

            data_str = line[5:].strip()
            if data_str == "[DONE]":
                break

            try:
                event = json.loads(data_str)
            except json.JSONDecodeError:
                continue

            event_type = event.get("event")
            answer = event.get("answer", "")

            if event_type == "agent_message":
                # Agent 应用的 agent_message 事件，answer 是增量文本，直接打印
                if answer:
                    print(answer, end="", flush=True)
            elif event_type == "message":
                # 普通 chat 应用的 message 事件，answer 是累计全文，只打印新增
                if len(answer) > printed:
                    print(answer[printed:], end="", flush=True)
                    printed = len(answer)
            elif event_type == "message_end":
                # 流结束，记录 conversation_id 便于多轮对话
                conversation_id = event.get("conversation_id", conversation_id)
            elif event_type == "error":
                print(f"\n[错误] {event.get('message', event)}", file=sys.stderr)

        print()   # 回答结束后换行

    return conversation_id


def main():
    parser = argparse.ArgumentParser(
        description="命令行与本地 Dify（Agent Chat App）进行流式对话",
    )
    parser.add_argument("query", nargs="*", help="要发送的问题；不传则从标准输入读取")
    parser.add_argument("--user", default=DIFY_USER, help="用户标识，默认 %(default)s")
    parser.add_argument("--conversation-id", default="", help="延续指定会话的多轮对话")
    args = parser.parse_args()

    query = " ".join(args.query).strip()
    if not query:
        # 无参数时从标准输入读取（支持管道输入）
        query = sys.stdin.read().strip()

    if not query:
        parser.error('请提供问题，例如：python dify_chat.py "你好"')

    conversation_id = stream_chat(query, user=args.user, conversation_id=args.conversation_id)
    if conversation_id:
        print(f"[conversation_id: {conversation_id}]", file=sys.stderr)


if __name__ == "__main__":
    main()
