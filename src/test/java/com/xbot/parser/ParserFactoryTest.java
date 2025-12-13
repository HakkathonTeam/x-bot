package com.xbot.parser;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ParserFactoryTest {

    @Test
    void getParseReturnsJsonParser() throws Exception {
        String json = loadResource("/exports/telegram-sample.json");

        ChatHistoryParser parser = ParserFactory.getParser(json);
        assertInstanceOf(JsonChatParser.class, parser);
    }

    @Test
    void getParserReturnsHtmlParser() throws Exception {
        String html = loadResource("/exports/telegram-sample.html");

        ChatHistoryParser parser = ParserFactory.getParser(html);
        assertInstanceOf(HtmlChatParser.class, parser);
    }

    @Test
    void getParserExceptionForEmptyContent() {
        assertThrows(ParserException.class, () -> ParserFactory.getParser("  "));
        assertThrows(ParserException.class, () -> ParserFactory.getParser("not a telegram export"));
    }

    @Test
    void getAllParsersContainsBothImplementations() {
        assertEquals(2, ParserFactory.getAllParsers().size());
        assertTrue(ParserFactory.getAllParsers().stream().anyMatch(p -> p instanceof JsonChatParser));
        assertTrue(ParserFactory.getAllParsers().stream().anyMatch(p -> p instanceof HtmlChatParser));
    }

    private static String loadResource(String path) throws Exception {
        try (var in = ParserFactoryTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
