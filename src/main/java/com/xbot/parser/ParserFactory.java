package com.xbot.parser;

import java.util.Arrays;
import java.util.List;

/**
 * Factory for selecting appropriate parser based on content format.
 * TODO: Implement by Alexey
 */
public class ParserFactory {

    private static final List<ChatHistoryParser> PARSERS = Arrays.asList(
            new JsonChatParser(),
            new HtmlChatParser()
    );

    public static ChatHistoryParser getParser(String content) throws ParserException {
        if (content == null || content.isBlank()) {
            throw new ParserException("Content is empty");
        }

        return PARSERS.stream()
                .filter(parser -> parser.canParse(content))
                .findFirst()
                .orElseThrow(() -> new ParserException("No parser found for this content"));
    }

    public static List<ChatHistoryParser> getAllParsers() {
        return PARSERS;
    }
}