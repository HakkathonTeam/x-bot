package com.xbot.parser;

import com.xbot.model.ExtractionResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JsonChatParserTest {

    private final JsonChatParser parser = new JsonChatParser();

    @Test
    void detectsTelegramJsonExport() throws Exception {
        String json = loadResource("/exports/telegram-sample.json");

        assertTrue(parser.canParse(json));
        assertFalse(parser.canParse("{\"messages\":{}}"));
        assertFalse(parser.canParse(""));
    }

    @Test
    void extractsParticipantsMentions() throws Exception {
        String json = loadResource("/exports/telegram-sample.json");

        ExtractionResult result = parser.parse(json);

        assertEquals(2, result.participants().size(), "participants: Alice + Bob");
        assertTrue(result.participants().stream().anyMatch(u -> "Alice".equals(u.name())));
        assertTrue(result.participants().stream().anyMatch(u -> "Bob".equals(u.name())));

        assertEquals(3, result.mentions().size(), "mentions: bob_user, carol, dave_user");
        assertTrue(result.mentions().stream().anyMatch(u -> "bob_user".equals(u.name())), "mentions: bob_user");
        assertTrue(result.mentions().stream().anyMatch(u -> "carol".equals(u.name())), "mentions: carol");
        assertTrue(result.mentions().stream().anyMatch(u -> "dave_user".equals(u.name())), "mentions: dave_user");

        assertEquals(1, result.channels().size());
        assertTrue(result.channels().stream().anyMatch(u -> "Channel Admin".equals(u.name())));
    }

    @Test
    void exceptionOnEmptyContent() {
        ParserException ex = assertThrows(ParserException.class, () -> parser.parse(" "));
        assertTrue(ex.getCause().getMessage().toLowerCase().contains("empty"));
    }

    private static String loadResource(String path) throws Exception {
        try (var in = JsonChatParserTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
