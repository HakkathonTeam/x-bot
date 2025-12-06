package com.xbot;

import com.xbot.bot.XBot;
import com.xbot.config.AppConfig;
import com.xbot.parser.ParserFactory;
import com.xbot.service.ExcelGenerator;
import com.xbot.service.MentionExtractor;
import com.xbot.service.UserExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

/**
 * Application entry point with manual dependency injection.
 */
public class XBotApplication {

    private static final Logger log = LoggerFactory.getLogger(XBotApplication.class);

    public static void main(String[] args) {
        log.info("Starting X-Bot...");

        try {
            // Load configuration
            AppConfig config = new AppConfig();
            log.info("Configuration loaded. Bot username: {} \t Bot token: {}", config.getBotUsername(), config.getBotToken());

            // Create services (manual DI)
            MentionExtractor mentionExtractor = new MentionExtractor();
            UserExtractor userExtractor = new UserExtractor(mentionExtractor);
            ExcelGenerator excelGenerator = new ExcelGenerator();
            ParserFactory parserFactory = new ParserFactory();

            // Create bot with all dependencies
            XBot bot = new XBot(config, parserFactory, userExtractor, excelGenerator);

            // Start bot
            try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
                botsApplication.registerBot(config.getBotToken(), bot);
                log.info("X-Bot started successfully!");

                // Keep running
                Thread.currentThread().join();
            }

        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Failed to start bot", e);
            System.exit(1);
        }
    }
}
