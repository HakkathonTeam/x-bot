package com.xbot.service;

/**
 * Service for extracting and deduplicating users from parsed data.
 * TODO: Implement by Nickolay
 */
public class UserExtractor {
    private final MentionExtractor mentionExtractor;
    public UserExtractor(MentionExtractor mentionExtractor) {
        this.mentionExtractor = mentionExtractor;
    }
}
