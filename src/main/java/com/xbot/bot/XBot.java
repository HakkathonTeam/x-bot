package com.xbot.bot;

import com.xbot.util.Constants;
import com.xbot.config.AppConfig;
import com.xbot.exception.FileSizeLimitExceededException;
import com.xbot.exception.InvalidFileFormatException;
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
import java.util.concurrent.TimeUnit;

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
        this.sessionService = new SessionService(config.getMaxFilesPerUser(), config.getSessionTimeoutMinutes());
        this.fileUploadService = new FileUploadService(telegramClient, sessionService, config.getMaxFileSizeBytes());

        // Добавляем shutdown hook для очистки временных файлов
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            fileUploadService.cleanupAllFiles();
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    // Задачи не завершились за 10 сек - отменяем
                    executorService.shutdownNow();

                    // Ждем еще немного после принудительной отмены
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.error("Executor did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt(); // Восстанавливаем флаг
            }
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
            sendMessage(chatId, Constants.REQUEST_MSG);
        }
    }

    private void handleCommand(Long chatId, Long userId, String command, String userName) {
        String cmd = command.split(" ")[0].toLowerCase();

        switch (cmd) {
            case Constants.START_CMD:
                sendWelcomeMessage(chatId, userName);
                break;
            case Constants.HELP_CMD:
                sendHelpMessage(chatId);
                break;
            case Constants.TEST_CMD:
                sendMessage(chatId, Constants.TEST_MSG_ANSWER);
                break;
            case Constants.FILES_CMD:
                showUploadedFiles(chatId, userId);
                break;
            case Constants.CLEAR_CMD:
                clearFiles(chatId, userId);
                break;
            default:
                sendMessage(chatId, Constants.UNKNOWN_MSG_ANSWER);
        }
    }

    private void handleDocumentMessage(Long chatId, Long userId, Document document) {


        // Проверяем количество уже загруженных файлов
        int fileCount = sessionService.getFileCount(userId);
        int maxFiles = config.getMaxFiles();


        if (fileCount >= config.getMaxFiles()) {
            sendMessage(chatId, String.format(Constants.ERROR_MSG_MAX_FILES, maxFiles, maxFiles));
            return;
        }

        // Отправляем сообщение о начале загрузки
        String fileName = document.getFileName();
        sendMessage(chatId, String.format(Constants.PROGRESS_MSG_WAIT, fileName));

        // Обрабатываем файл асинхронно
        executorService.submit(() -> {
            try {
                UploadedFile uploadedFile = fileUploadService.downloadFile(userId, document);

                // Отправляем сообщение об успешной загрузке
                String response = String.format(Constants.SUCCESSFUL_MSG,
                        uploadedFile.getFileName(),
                        uploadedFile.isHtmlFile() ? "HTML" : "JSON",
                        uploadedFile.getFileSize() / 1024,
                        sessionService.getFileCount(userId),
                        maxFiles);

                sendMessage(chatId, response);

            } catch (InvalidFileFormatException e) {
                sendMessage(chatId, Constants.ERROR_MSG_WRONG_FORMAT);
            } catch (FileSizeLimitExceededException e) {
                sendMessage(chatId, String.format(Constants.ERROR_MSG_MAX_FILE_SIZE,
                        document.getFileName(), config.getMaxFileSizeMB()));
            }catch (Exception e) {
                log.error("Failed to download file for user {}", userId, e);
                sendMessage(chatId, Constants.ERROR_MSG_UNKNOWN_DOWNLOAD);
            }
        });
    }


    private void sendWelcomeMessage(Long chatId, String userName) {

        sendMessage(chatId, String.format(Constants.WELCOME_MSG, userName, config.getMaxFiles()));
    }

    private void sendHelpMessage(Long chatId) {

        sendMessage(chatId, Constants.HELP_MSG);
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
            sendMessage(chatId, Constants.NO_FILES_MSG);
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append(String.format(Constants.FILES_MSG, fileCount));

        var files = sessionService.getFiles(userId);
        for (int i = 0; i < files.size(); i++) {
            UploadedFile file = files.get(i);
            message.append(String.format("%d. %s\n", i + 1, file.getFileName()));
            message.append(String.format("   📊 %s | 💾 %d KB\n",
                    file.isHtmlFile() ? "HTML" : "JSON",
                    file.getFileSize() / 1024));
        }

        message.append(String.format(Constants.LAST_FILES_MSG, config.getMaxFiles() - fileCount));

        sendMessage(chatId, message.toString());
    }

    private void clearFiles(Long chatId, Long userId) {
        int fileCount = sessionService.getFileCount(userId);

        if (fileCount == 0) {
            sendMessage(chatId, Constants.NO_FILES_FOR_CLEAN_MSG);
            return;
        }

        fileUploadService.cleanupUserFiles(userId);
        sendMessage(chatId, String.format(
                Constants.DELETED_FILES_MSG, fileCount));
    }

}
