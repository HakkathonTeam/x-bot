package com.xbot.model;

import java.util.Set;

/**
 * Unified result of extracting users from chat history.
 * Used by both JsonChatParser and HtmlChatParser.
 * Contains chat metadata and extracted user collections.
 */
public record ExtractionResult(
        // Chat metadata
        String chatName,
        String chatType,
        Long chatId,          // nullable - HTML exports don't have this

        // Extracted user data
        Set<User> participants,
        Set<User> mentions,
        Set<User> channels
) {
    /**
     * Convenience constructor without chat metadata.
     */
    public ExtractionResult(Set<User> participants, Set<User> mentions, Set<User> channels) {
        this(null, null, null, participants, mentions, channels);
    }

    /**
     * Convenience constructor with chat name only.
     */
    public ExtractionResult(String chatName, Set<User> participants, Set<User> mentions, Set<User> channels) {
        this(chatName, null, null, participants, mentions, channels);
    }
}
