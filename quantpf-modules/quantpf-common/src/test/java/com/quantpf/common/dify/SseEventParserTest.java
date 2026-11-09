package com.quantpf.common.dify;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SseEventParser 单元测试：覆盖一次性解析、增量（分片）解析、多行 data、\r\n、注释行、retry 等场景。
 */
class SseEventParserTest {

    @Test
    void parseSingleEvent() {
        List<DifySseEvent> events = SseEventParser.parse("event: message\ndata: {\"answer\":\"hi\"}\n\n");
        assertEquals(1, events.size());
        assertEquals("message", events.get(0).event());
        assertEquals("{\"answer\":\"hi\"}", events.get(0).data());
    }

    @Test
    void parseMultipleEventsWithCommentAndId() {
        String raw = ": comment line\n"
            + "event: message\n"
            + "id: 1\n"
            + "data: first\n"
            + "\n"
            + "event: message_end\n"
            + "data: {\"task_id\":\"t-1\"}\n"
            + "\n";
        List<DifySseEvent> events = SseEventParser.parse(raw);
        assertEquals(2, events.size());
        assertEquals("1", events.get(0).id());
        assertEquals("first", events.get(0).data());
        assertEquals("message_end", events.get(1).event());
    }

    @Test
    void parseMultiLineData() {
        List<DifySseEvent> events = SseEventParser.parse("data: a\ndata: b\n\n");
        assertEquals(1, events.size());
        assertEquals("a\nb", events.get(0).data());
    }

    @Test
    void parseCrlfAndDefaultEventName() {
        List<DifySseEvent> events = SseEventParser.parse("data: x\r\n\r\n");
        assertEquals(1, events.size());
        assertEquals("message", events.get(0).event());
        assertEquals("x", events.get(0).data());
    }

    @Test
    void ignoresRetryField() {
        List<DifySseEvent> events = SseEventParser.parse("retry: 1000\nevent: ping\ndata: {}\n\n");
        assertEquals(1, events.size());
        assertEquals(1000L, events.get(0).retry());
    }

    @Test
    void incrementalParsingAcrossChunks() {
        SseEventParser.Parser parser = SseEventParser.newParser();
        // 事件被切在字段名 / 字段值中间
        parser.push("event: mess");
        assertTrue(parser.drain().isEmpty());
        parser.push("age\ndata: {\"a");
        parser.push("\":1}\n\n");
        List<DifySseEvent> events = parser.drain();
        assertEquals(1, events.size());
        assertEquals("message", events.get(0).event());
        assertEquals("{\"a\":1}", events.get(0).data());
    }

    @Test
    void incrementalFinishFlushesLastEventWithoutTrailingBlankLine() {
        SseEventParser.Parser parser = SseEventParser.newParser();
        parser.push("event: message\ndata: hello");
        List<DifySseEvent> events = parser.finish();
        assertEquals(1, events.size());
        assertEquals("hello", events.get(0).data());
    }

    @Test
    void consecutiveBlankLinesDoNotProduceEmptyEvents() {
        List<DifySseEvent> events = SseEventParser.parse("data: a\n\n\n\n");
        assertEquals(1, events.size());
        assertEquals("a", events.get(0).data());
    }
}
