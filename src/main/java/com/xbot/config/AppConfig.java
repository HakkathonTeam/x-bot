package com.xbot.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application configuration loaded from environment variables and .env file.
 */
public class AppConfig {
    private final String botUsername;
    private final String botToken;
    private final int maxFiles;
    private final int maxFileSizeMB;
    private final int maxFilesPerUser;
    private final int sessionTimeoutMinutes;
    private final int processingTimeoutMs;

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private final Dotenv dotenv;

    public AppConfig() {
        // Load .env file (ignores if missing, falls back to system env)
        this.dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        this.botUsername = getEnv("BOT_USERNAME");
        this.botToken = getEnv("BOT_TOKEN");
        this.maxFiles = getEnvAsInt("MAX_FILES", 10);
        this.maxFileSizeMB = getEnvAsInt("MAX_FILE_SIZE_MB", 50);
        this.maxFilesPerUser = getEnvAsInt("MAX_FILES_PER_USER", 10);
        this.sessionTimeoutMinutes = getEnvAsInt("SESSION_TIMEOUT_MINUTES", 30);
        this.processingTimeoutMs = getEnvAsInt("PROCESSING_TIMEOUT_MS", 3000);

        if (botUsername == null || botUsername.isBlank()) {
            throw new IllegalStateException("Configuration error: BOT_USERNAME environment variable is not set");
        }
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("Configuration error: BOT_TOKEN environment variable is not set");
        }
    }

    private String getEnv(String name) {
        return dotenv.get(name);
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

    public long getMaxFileSizeBytes() {
        return (long) maxFileSizeMB * 1024 * 1024;
    }

    public int getMaxFileSizeMB() { return maxFileSizeMB; }

    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public int getMaxFilesPerUser() { return maxFilesPerUser; }
    public int getProcessingTimeoutMs() { return processingTimeoutMs; }

    private int getEnvAsInt(String name, int defaultValue) {
        String value = getEnv(name);
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
