package com.xbot.config;

import org.apache.commons.lang3.NotImplementedException;

/**
 * Application configuration loaded from environment variables.
 */
public class AppConfig {
    private final String botUsername;
    private final String botToken;

    public AppConfig() {
        // Читаем из переменных окружения
        this.botUsername = System.getenv("BOT_USERNAME");
        this.botToken = System.getenv("BOT_TOKEN");

        // Проверяем, что переменные установлены
        if (botUsername == null || botUsername.isBlank()) {
            throw new IllegalStateException("BOT_USERNAME environment variable is not set");
        }
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("BOT_TOKEN environment variable is not set");
        }
    }

    public String getBotUsername() {
        return botUsername;
    }

    public String getBotToken() {
        return botToken;
    }
}
