package com.xbot.model;

import java.util.Set;

/**
 * Result of extracting users from chat history.
 * Contains separate collections for participants, mentions, and channels.
 * TODO: Implement by Nickolay
 */
public record ExtractionResult(
        Set<User> participants,
        Set<User> mentions,
        Set<User> channels
) {}
