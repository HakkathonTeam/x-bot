package com.xbot.model;

/**
 * Represents a Telegram user extracted from chat history.
 * Uses id for equality/deduplication.
 * TODO: Implement by Nickolay
 */
public record User(String displayName) {}
