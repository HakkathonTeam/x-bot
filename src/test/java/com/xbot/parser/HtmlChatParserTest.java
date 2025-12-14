package com.xbot.parser;

import com.xbot.model.ExtractionResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HtmlChatParserTest {

    private final HtmlChatParser parser = new HtmlChatParser();

    @Test
    void detectsTelegramHtmlExport() throws Exception {
        String html = loadResource("/exports/telegram-sample.html");

        assertTrue(parser.canParse(html));
        assertFalse(parser.canParse("{"));
        assertFalse(parser.canParse(null));
    }

    @Test
    void extractsParticipantsAndMentions() throws Exception {
        String html = loadResource("/exports/telegram-sample.html");

        ExtractionResult result = parser.parse(html);

        assertEquals(2, result.participants().size(), "participants: Alice + Bob (deleted/service ignored)");
        assertTrue(result.participants().stream().anyMatch(u -> "Alice".equals(u.name())));
        assertTrue(result.participants().stream().anyMatch(u -> "Bob".equals(u.name())));

        assertEquals(2, result.mentions().size(), "mentions: bob_user + carol (merged across text blocks)");
        assertTrue(result.mentions().stream().anyMatch(u -> "bob_user".equals(u.name())));
        assertTrue(result.mentions().stream().anyMatch(u -> "carol".equals(u.name())));
    }

    @Test
    void exceptionOnEmptyContent() {
        assertThrows(ParserException.class, () -> parser.parse("\n\t "));
    }

    private static String loadResource(String path) throws Exception {
        try (var in = HtmlChatParserTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
