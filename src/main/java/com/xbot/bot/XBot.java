package com.xbot.bot;

import com.xbot.config.AppConfig;
import com.xbot.parser.ParserFactory;
import com.xbot.service.ExcelGenerator;
import com.xbot.service.UserExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Main Telegram bot class.
 * TODO: Implement by Vladimir
 */
public class XBot implements LongPollingSingleThreadUpdateConsumer {
    private final AppConfig config;
    private final ParserFactory parserFactory;
    private final UserExtractor userExtractor;
    private final ExcelGenerator excelGenerator;
    private TelegramClient telegramClient;

    private static final Logger log = LoggerFactory.getLogger(XBot.class);

    public XBot(AppConfig config,
                ParserFactory parserFactory,
                UserExtractor userExtractor,
                ExcelGenerator excelGenerator) {
        this.config = config;
        this.parserFactory = parserFactory;
        this.userExtractor = userExtractor;
        this.excelGenerator = excelGenerator;

        this.telegramClient = new OkHttpTelegramClient(config.getBotToken());
    }

    @Override
    public void consume(Update update) {
        log.debug("Received update: {}", update.getUpdateId());

        // Проверяем, есть ли сообщение
        if (!update.hasMessage()) {
            log.debug("Update doesn't contain a message");
            return;
        }

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText();

        log.info("Message from {} ({}): {}",
                message.getFrom().getFirstName(),
                chatId,
                text);

        // Обработка команд
        if (text != null && text.startsWith("/")) {
            handleCommand(chatId, text, message.getFrom().getFirstName());
        } else if (message.hasText()) {
            // Эхо-ответ для тестирования
            sendEchoMessage(chatId, text);
        } else if (message.hasDocument()) {
            handleDocument(message);
        } else {
            sendMessage(chatId, "Отправьте мне файлы экспорта чата (HTML/JSON) или используйте команды:\n/start - начать\n/help - помощь");
        }
    }

    private void handleCommand(Long chatId, String command, String userName) {
        String cmd = command.split(" ")[0].toLowerCase();

        switch (cmd) {
            case "/start":
                sendWelcomeMessage(chatId, userName);
                break;
            case "/help":
                sendHelpMessage(chatId);
                break;
            case "/test":
                sendMessage(chatId, "✅ Бот работает! Тестовое сообщение получено.");
                break;
            case "/echo":
                if (command.length() > 6) {
                    sendMessage(chatId, command.substring(6));
                } else {
                    sendMessage(chatId, "Напишите /echo <текст>");
                }
                break;
            default:
                sendMessage(chatId, "Неизвестная команда. Используйте /help для списка команд.");
        }
    }

    private void sendWelcomeMessage(Long chatId, String userName) {
        String welcome = String.format("""
            👋 Привет, %s!
            
            Я - XBot для анализа экспорта чатов Telegram.
            
            Отправьте мне файлы экспорта в формате HTML или JSON (до 10 файлов одновременно), и я создам отчет об участниках чата.
            
            Используйте команды:
            /help - показать справку
            /test - проверить работу бота
            /echo <текст> - эхо-ответ для тестирования
            
            Готов к работе! 🚀
            """, userName);

        sendMessage(chatId, welcome);
    }

    private void sendHelpMessage(Long chatId) {
        String help = """
            📚 Справка по командам:
            
            /start - Начальное приветствие
            /help - Эта справка
            /test - Проверить работу бота
            /echo <текст> - Тестовая эхо-функция
            
            Как использовать:
            1. Экспортируйте историю чата из Telegram (Settings → Advanced → Export chat history)
            2. Отправьте полученные файлы (HTML/JSON) этому боту
            3. Получите отчет об участниках чата
            
            Формат вывода:
            • Менее 50 участников - текстовый список
            • 50+ участников - файл Excel
            
            Ограничения:
            • Максимум 10 файлов за раз
            • Форматы: HTML, JSON
            """;

        sendMessage(chatId, help);
    }

    private void sendEchoMessage(Long chatId, String text) {
        String response = String.format("""
            📨 Эхо-ответ:
            
            Вы написали: %s
            
            Длина: %d символов
            
            Тест связи: ✅ Успешно
            """, text, text.length());

        sendMessage(chatId, response);
    }

    private void handleDocument(Message message) {
        Long chatId = message.getChatId();
        String fileName = message.getDocument().getFileName();

        log.info("Received document: {} from {}", fileName, chatId);

        // Временный ответ
        sendMessage(chatId, String.format("📎 Получен файл: %s\n\n⏳ Обработка файлов будет реализована в следующих этапах.", fileName));
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();

        try {
            telegramClient.execute(sendMessage);
            log.debug("Message sent to chat {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage(), e);
        }
    }
}
