package com.xbot.config;

import com.xbot.bot.XBot;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application configuration loaded from environment variables.
 */
public class AppConfig {
    private final String botUsername;
    private final String botToken;
    private final int maxFiles;
    private final int maxFileSizeMB;

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    public AppConfig() {
        // Читаем из переменных окружения
        this.botUsername = System.getenv("BOT_USERNAME");
        this.botToken = System.getenv("BOT_TOKEN");
        this.maxFiles = getEnvAsInt("MAX_FILES", 10);
        this.maxFileSizeMB = getEnvAsInt("MAX_FILE_SIZE_MB", 50);

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

    public int getMaxFiles() {
        return maxFiles;
    }

    public int getMaxFileSizeMB() {
        return maxFileSizeMB;
    }

    private int getEnvAsInt(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid value for {}: {}, using default: {}", name, value, defaultValue);
            }
        }
        return defaultValue;
    }
}
