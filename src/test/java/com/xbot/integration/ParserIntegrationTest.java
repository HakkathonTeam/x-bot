package com.xbot.integration;

import com.xbot.model.ExtractionResult;
import com.xbot.parser.ParserFactory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ParserIntegrationTest {

    @Test
    void parsesJsonExportE2E() throws Exception {
        String content = loadResource("/exports/telegram-sample.json");

        ExtractionResult result = ParserFactory.getParser(content).parse(content);

        assertEquals(2, result.participants().size());
        assertEquals(3, result.mentions().size());
        assertEquals(1, result.channels().size());
    }

    @Test
    void parsesHtmlExportE2E() throws Exception {
        String content = loadResource("/exports/telegram-sample.html");

        ExtractionResult result = ParserFactory.getParser(content).parse(content);

        assertEquals(2, result.participants().size());
        assertEquals(2, result.mentions().size());
        assertEquals(0, result.channels().size());
    }

    private static String loadResource(String path) throws Exception {
        try (var in = ParserIntegrationTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
