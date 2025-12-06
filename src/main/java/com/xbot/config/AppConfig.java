package com.xbot.config;

import org.apache.commons.lang3.NotImplementedException;

/**
 * Application configuration loaded from environment variables.
 */
public class AppConfig {
    public String getBotUsername() {
        return "BOT_USER_NAME";
    }

    public String getBotToken() {
        return "";
    }
}
