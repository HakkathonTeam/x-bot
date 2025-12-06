package com.xbot.model;

/**
 * Represents a Telegram user extracted from chat history.
 * Uses id for equality/deduplication.
 */
public record User(
        String telegramId,
        String name,
        String fullName
) {
    public User(String telegramId, String name) {
        this(telegramId, name, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return telegramId.equals(user.telegramId);
    }

    @Override
    public int hashCode() {
        return telegramId.hashCode();
    }
}