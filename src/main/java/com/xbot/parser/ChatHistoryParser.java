package com.xbot.parser;

import com.xbot.model.ExtractionResult;

/**
 * Interface for parsing Telegram chat history exports.
 * Implementations handle specific formats (JSON, HTML).
 */
public interface ChatHistoryParser {

    /**
     * Parses chat history content and extracts users.
     *
     * @param content the raw content of the chat export file
     * @return extraction result containing participants, mentions, and channels
     * @throws ParserException if parsing fails
     */
    ExtractionResult parse(String content) throws ParserException;

    /**
     * Checks if this parser can handle the given content.
     *
     * @param content the raw content to check
     * @return true if this parser can handle the content
     */
    boolean canParse(String content);

    /**
     * Returns the format name this parser handles.
     *
     * @return format name (e.g., "JSON", "HTML")
     */
    String getFormatName();
}
