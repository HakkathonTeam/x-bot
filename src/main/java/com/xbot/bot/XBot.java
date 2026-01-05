package com.xbot.bot;

import com.xbot.model.UploadedFile;
import com.xbot.model.User;
import com.xbot.parser.ParserException;
import com.xbot.util.Constants;
import com.xbot.config.AppConfig;
import com.xbot.exception.FileSizeLimitExceededException;
import com.xbot.exception.InvalidFileFormatException;
import com.xbot.parser.ParserFactory;
import com.xbot.service.ExcelGenerator;
import com.xbot.service.FileUploadService;
import com.xbot.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main Telegram bot class.
 * TODO: Implement by Vladimir
 */
public class XBot implements LongPollingSingleThreadUpdateConsumer, SessionService.ProcessingCallback {
    private final AppConfig config;
    private final ExcelGenerator excelGenerator;
    private final TelegramClient telegramClient;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final SessionService sessionService;
    private final FileUploadService fileUploadService;

    private static final Logger log = LoggerFactory.getLogger(XBot.class);

    public XBot(AppConfig config,
                ExcelGenerator excelGenerator) {
        this.config = config;
        this.excelGenerator = excelGenerator;

        this.telegramClient = new OkHttpTelegramClient(config.getBotToken());

        // Инициализируем сервисы
        this.sessionService = new SessionService(config.getMaxFilesPerUser(), config.getSessionTimeoutMinutes(), config.getProcessingTimeoutMs());
        this.fileUploadService = new FileUploadService(telegramClient, sessionService, config.getMaxFileSizeBytes());

        this.sessionService.setProcessingCallback(this);
        // Добавляем shutdown hook для очистки временных файлов
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.sessionService.stopAllTimers();
            this.sessionService.cleanAllFiles();
            this.fileUploadService.deleteTempDir();
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
            handleCommand(chatId, text, message.getFrom().getFirstName());
        } else if (message.hasDocument()) {
            handleDocumentMessage(chatId, userId, message.getDocument());
        } else {
            sendMessage(chatId, Constants.REQUEST_MSG);
        }
    }

    private void handleCommand(Long chatId, String command, String userName) {
        String cmd = command.split(" ")[0].toLowerCase();

        switch (cmd) {
            case Constants.START_CMD:
                sendWelcomeMessage(chatId, userName);
                break;
            case Constants.HELP_CMD:
                sendHelpMessage(chatId);
                break;
            default:
                sendMessage(chatId, Constants.UNKNOWN_MSG_ANSWER);
        }
    }

    private void handleDocumentMessage(Long chatId, Long userId, Document document) {

        // Проверяем количество уже загруженных файлов
        int fileCount = sessionService.getFileCount(userId, chatId);
        int maxFiles = config.getMaxFiles();

        if (fileCount >= maxFiles) {
            if (!sessionService.maxFilesErrorMsgWasSend(userId, chatId)) {
                sessionService.setMaxFilesErrorMsgWasSend(userId, chatId);
                sendMessage(chatId, String.format(Constants.ERROR_MSG_MAX_FILES, maxFiles, maxFiles));
            }
            return;
        } else {
            sessionService.increaseFileCount(userId, chatId);
        }

        if (sessionService.isBusy(userId, chatId)) {
            sendMessage(chatId, Constants.ERROR_WAIT_FOR_PREVIOUS_REQUEST);
            return;
        }

        // Отправляем сообщение о начале загрузки
        if (sessionService.isIdle(userId, chatId)) {
            sessionService.setUploadBegin(userId, chatId);
            sendMessage(chatId, String.format(Constants.PROGRESS_MSG_WAIT));
        }
        // Обрабатываем файл асинхронно
        executorService.submit(() -> {
            try {
                fileUploadService.downloadFile(userId, chatId, document);
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

    public void sendFileToUser(Long chatId, File fileToSend) throws TelegramApiException {
        InputFile inputFile = new InputFile(fileToSend, fileToSend.getName());

        SendDocument sendDocument = new SendDocument(chatId.toString(), inputFile);
        sendDocument.setCaption("Файл с результатом");
        telegramClient.execute(sendDocument);
    }

    @Override
    public boolean onProcessingBegin(Long userId, Long chatId, List<UploadedFile> files) throws Exception {
        Set<User> participants = new HashSet<>();
        Set<User> mentions = new HashSet<>();
        Set<User> channels = new HashSet<>();
        sendMessage(chatId, Constants.PROCESS_BEGIN);

        for (var f : files) {
            var content = Files.readString(Paths.get(f.getLocalPath()));
            try {
                var parse = ParserFactory.getParser(content).parse(content);
                participants.addAll(parse.participants());
                mentions.addAll(parse.mentions());
                channels.addAll(parse.channels());
            } catch (ParserException e) {
                log.warn("Parser error file: {}", f.getFileName());
                sendMessage(chatId, String.format(Constants.ERROR_FILE_PROCESS, f.getFileName()));
                return false;
            }
        }

        if (participants.isEmpty() && mentions.isEmpty() && channels.isEmpty()) {
            log.warn("Empty users list");
            sendMessage(chatId, Constants.WARNING_USERS_LIST_EMPTY);
        }

        int totalCount = participants.size() + mentions.size() + channels.size();
        if (totalCount < Constants.TEXT_OUTPUT_THRESHOLD) {
            sendMessage(chatId, formatResultAsText(participants, mentions, channels));
        } else {
            File resultFile = excelGenerator.generateExcel(participants, mentions, channels,
                    "result", fileUploadService.getTempDirectory());
            sendFileToUser(chatId, resultFile);
            Files.deleteIfExists(resultFile.toPath());
        }
        return true;
    }

    private String formatResultAsText(Set<User> participants, Set<User> mentions, Set<User> channels) {
        StringBuilder sb = new StringBuilder();
        int totalCount = participants.size() + mentions.size() + channels.size();
        sb.append(String.format("👥 Найдено: %d\n", totalCount));

        if (!participants.isEmpty()) {
            sb.append(String.format("\n📝 Участники (%d):\n", participants.size()));
            int counter = 1;
            for (User user : participants) {
                sb.append(String.format("%d. %s (%s)\n", counter++, user.fullName(), formatUserLink(user)));
            }
        }

        if (!mentions.isEmpty()) {
            sb.append(String.format("\n💬 Упоминания (%d):\n", mentions.size()));
            int counter = 1;
            for (User user : mentions) {
                sb.append(String.format("%d. %s (%s)\n", counter++, user.fullName(), formatUserLink(user)));
            }
        }

        if (!channels.isEmpty()) {
            sb.append(String.format("\n📢 Каналы (%d):\n", channels.size()));
            int counter = 1;
            for (User user : channels) {
                sb.append(String.format("%d. %s\n", counter++, user.fullName()));
            }
        }

        return sb.toString();
    }

    private String formatUserLink(User user) {
        if (user.telegramId() == null) {
            return "";
        }
        return user.telegramId().startsWith("user")
                ? "tg://user?id=" + user.telegramId().substring(4)
                : "@" + user.telegramId();
    }

    @Override
    public void onProcessingComplete(Long chatId) {
        sendMessage(chatId, Constants.PROCESS_COMPLETE);
    }

    @Override
    public void onProcessingError(Long chatId) {
        sendMessage(chatId, Constants.ERROR_PROCESS);
    }
}
