package com.xbot.bot;

import com.xbot.config.AppConfig;
import com.xbot.model.UploadedFile;
import com.xbot.parser.ParserFactory;
import com.xbot.service.ExcelGenerator;
import com.xbot.service.FileUploadService;
import com.xbot.service.SessionService;
import com.xbot.service.UserExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final SessionService sessionService;
    private final FileUploadService fileUploadService;

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

        // Инициализируем сервисы
        this.sessionService = new SessionService();
        this.fileUploadService = new FileUploadService(telegramClient, sessionService);

        // Добавляем shutdown hook для очистки временных файлов
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            fileUploadService.cleanupAllFiles();
            executorService.shutdown();
        }));
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
        Long userId = message.getFrom().getId();

        log.info("Message from {} ({}): {}",
                message.getFrom().getFirstName(),
                chatId,
                text);

        // Обработка команд
        if (text != null && text.startsWith("/")) {
            handleCommand(chatId, userId, text, message.getFrom().getFirstName());
        } else if (message.hasDocument()) {
            handleDocumentMessage(chatId, userId, message.getDocument());
        } else {
            sendMessage(chatId, "Отправьте мне файлы экспорта чата (HTML/JSON) или используйте команды:\n/start - начать\n/help - помощь");
        }
    }

    private void handleCommand(Long chatId, Long userId, String command, String userName) {
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
            case "/files":
                showUploadedFiles(chatId, userId);
                break;
            case "/clear":
                clearFiles(chatId, userId);
                break;
            default:
                sendMessage(chatId, "Неизвестная команда. Используйте /help для списка команд.");
        }
    }

    private void handleDocumentMessage(Long chatId, Long userId, Document document) {
        // Проверяем количество уже загруженных файлов
        int fileCount = sessionService.getFileCount(userId);
        int maxFiles = config.getMaxFiles();
        if (fileCount >= config.getMaxFiles()) {
            sendMessage(chatId, String.format(
                    "❌ Вы уже загрузили максимальное количество файлов (%d).\n" +
                            "Используйте /clear чтобы очистить или отправляйте файлы пачками до %d штук.",
                    maxFiles, maxFiles));
            return;
        }

        // Отправляем сообщение о начале загрузки
        String fileName = document.getFileName();
        sendMessage(chatId, String.format(
                "📥 Загружаю файл: %s\n" +
                        "⏳ Пожалуйста, подождите...",
                fileName));

        // Обрабатываем файл асинхронно
        executorService.submit(() -> {
            try {
                UploadedFile uploadedFile = fileUploadService.downloadFile(userId, document);

                // Отправляем сообщение об успешной загрузке
                String response = String.format(
                        "✅ Файл загружен: %s\n" +
                                "📊 Формат: %s\n" +
                                "💾 Размер: %d KB\n" +
                                "📁 Всего файлов: %d/%d\n\n" +
                                "Отправьте ещё файлы или используйте команды:\n" +
                                "/files - показать все файлы\n" +
                                "/clear - очистить\n" +
                                "/help - справка",
                        uploadedFile.getFileName(),
                        uploadedFile.isHtmlFile() ? "HTML" : "JSON",
                        uploadedFile.getFileSize() / 1024,
                        sessionService.getFileCount(userId),
                        maxFiles);

                sendMessage(chatId, response);

            } catch (IllegalArgumentException e) {
                sendMessage(chatId, "❌ Ошибка: " + e.getMessage() +
                        "\nПоддерживаются только HTML и JSON файлы.");
            } catch (Exception e) {
                log.error("Failed to download file for user {}", userId, e);
                sendMessage(chatId, "❌ Не удалось загрузить файл. Попробуйте ещё раз.");
            }
        });
    }


    private void sendWelcomeMessage(Long chatId, String userName) {
        String welcome = String.format("""
            👋 Привет, %s!
            
            Я - XBot для анализа экспорта чатов Telegram.
            
            **Как использовать:**
            1. Экспортируйте историю чата из Telegram
            2. Отправьте мне полученные файлы (HTML/JSON)
            3. Я проанализирую файлы и создам отчет
            
            **Ограничения:**
            • Максимум %d файлов за раз
            • Форматы: HTML, JSON
            
            **Команды:**
            /help - полная справка
            /files - показать загруженные файлы
            /clear - очистить файлы
            
            Готов к работе! 🚀
            """, userName, config.getMaxFiles());

        sendMessage(chatId, welcome);
    }

    private void sendHelpMessage(Long chatId) {
        String help = """
            📚 Справка по командам:
            
            /start - Начальное приветствие
            /help - Эта справка
            /files - Показать загруженные файлы
            /clear - Очистить файлы
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

    private void showUploadedFiles(Long chatId, Long userId) {
        int fileCount = sessionService.getFileCount(userId);

        if (fileCount == 0) {
            sendMessage(chatId, "📭 У вас нет загруженных файлов.\n" +
                    "Отправьте мне файлы экспорта чата (HTML/JSON).");
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append(String.format("📁 Загруженные файлы (%d):\n\n", fileCount));

        var files = sessionService.getFiles(userId);
        for (int i = 0; i < files.size(); i++) {
            UploadedFile file = files.get(i);
            message.append(String.format("%d. %s\n", i + 1, file.getFileName()));
            message.append(String.format("   📊 %s | 💾 %d KB\n",
                    file.isHtmlFile() ? "HTML" : "JSON",
                    file.getFileSize() / 1024));
        }

        message.append("\n👆 Можно отправить ещё ").append(config.getMaxFiles() - fileCount).append(" файлов");

        sendMessage(chatId, message.toString());
    }

    private void clearFiles(Long chatId, Long userId) {
        int fileCount = sessionService.getFileCount(userId);
        if (fileCount == 0) {
            sendMessage(chatId, "📭 Нет файлов для очистки.");
            return;
        }

        fileUploadService.cleanupUserFiles(userId);
        sendMessage(chatId, String.format(
                "🗑️ Удалено %d файлов.\nТеперь можно загружать новые файлы.", fileCount));
    }

}
