package com.xbot.model;

import java.util.Objects;

/**
 * Represents a Telegram user extracted from chat history.
 * Uses telegramId for equality/deduplication.
 */
public record User(
        String telegramId,
        String username,
        String name,
        String fullName
) {
    /**
     * Constructor for HTML parsing where only display name is available.
     * Uses displayName as both telegramId and name.
     */
    public User(String displayName) {
        this(displayName, null, displayName, displayName);
    }

    /**
     * Constructor with telegramId and name (fullName defaults to name).
     */
    public User(String telegramId, String name) {
        this(telegramId, null, name, name);
    }

    /**
     * Constructor with telegramId, name and fullName (no username).
     */
    public User(String telegramId, String name, String fullName) {
        this(telegramId, null, name, fullName);
    }

    /**
     * Returns the username without @ prefix if available.
     */
    public String getUsernameClean() {
        if (username == null) return null;
        return username.startsWith("@") ? username.substring(1) : username;
    }

    /**
     * Checks if this user has a numeric telegramId (starts with "user").
     */
    public boolean hasNumericId() {
        return telegramId != null && telegramId.startsWith("user");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(telegramId, user.telegramId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(telegramId);
    }
}
