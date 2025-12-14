package com.xbot.integration;

import com.xbot.bot.XBot;
import com.xbot.config.AppConfig;
import com.xbot.model.UploadedFile;
import com.xbot.service.ExcelGenerator;
import com.xbot.service.FileUploadService;
import com.xbot.service.SessionService;
import com.xbot.util.Constants;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BotIntegrationTest {

    private final TelegramClient telegramClient = mock(TelegramClient.class);
    private final ExcelGenerator excelGenerator = mock(ExcelGenerator.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final FileUploadService fileUploadService = mock(FileUploadService.class);

    @Test
    void startCommand() throws Exception {
        XBot bot = buildBotForTest();

        Update u = updateWithText(42L, 1001L, "Alex", "/start");
        when(telegramClient.execute(any(SendMessage.class))).thenReturn(null);

        bot.consume(u);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, times(1)).execute(captor.capture());

        SendMessage sent = captor.getValue();
        assertEquals("42", sent.getChatId());
        assertNotNull(sent.getText());
        assertTrue(sent.getText().contains("Alex"), "Welcome text should include user's first name");
    }

    @Test
    void plainTextWithoutDocument() throws Exception {
        XBot bot = buildBotForTest();

        Update u = updateWithText(42L, 1001L, "Alex", "hello");
        when(telegramClient.execute(any(SendMessage.class))).thenReturn(null);

        bot.consume(u);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        assertEquals(Constants.REQUEST_MSG, captor.getValue().getText());
    }

    @Test
    void documentWhenMaxFilesReached() throws Exception {
        XBot bot = buildBotForTest();

        AppConfig cfg = getConfigFromBot(bot);
        int maxFiles = cfg.getMaxFiles();
        when(sessionService.getFileCount(1001L, 42L)).thenReturn(maxFiles);
        when(telegramClient.execute(any(SendMessage.class))).thenReturn(null);

        Update u = updateWithDocument(42L, 1001L, "Alex", "export.json", 12345L);
        bot.consume(u);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        String text = captor.getValue().getText();

        assertNotNull(text);
        assertTrue(text.contains(String.valueOf(cfg.getMaxFiles())),
                "Expected max-files error to mention the configured limit");
    }

    @Test
    void documentDownloadHappyPath() throws Exception {
        XBot bot = buildBotForTest();

        when(sessionService.getFileCount(1001L, 42L)).thenReturn(0, 1);
        when(sessionService.isBusy(1001L, 42L)).thenReturn(false);
        when(telegramClient.execute(any(SendMessage.class))).thenReturn(null);

        UploadedFile uploaded = new UploadedFile("export.json", Path.of("/tmp/export.json").toString(), "text/plain", 2048);
        when(fileUploadService.downloadFile(eq(1001L), eq(42L), any(Document.class))).thenReturn(uploaded);

        Update u = updateWithDocument(42L, 1001L, "Alex", "export.json", 2048L);
        bot.consume(u);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient, times(2)).execute(captor.capture());

        List<SendMessage> sent = captor.getAllValues();
        assertTrue(sent.get(0).getText().contains("export.json"), "progress message should mention filename");
        assertTrue(sent.get(1).getText().contains("export.json"), "success message should mention filename");
    }

    private XBot buildBotForTest() throws Exception {
        AppConfig cfg = mock(AppConfig.class);

        when(cfg.getBotToken()).thenReturn("TEST_TOKEN");
        when(cfg.getBotUsername()).thenReturn("test-bot");

        when(cfg.getMaxFiles()).thenReturn(2);
        when(cfg.getMaxFilesPerUser()).thenReturn(2);
        when(cfg.getSessionTimeoutMinutes()).thenReturn(5);
        when(cfg.getProcessingTimeoutMs()).thenReturn(10_000);

        when(cfg.getMaxFileSizeBytes()).thenReturn(5L * 1024 * 1024);

        XBot bot = new XBot(cfg, excelGenerator);

        setField(bot, "telegramClient", telegramClient);
        setField(bot, "sessionService", sessionService);
        setField(bot, "fileUploadService", fileUploadService);
        setField(bot, "executorService", new DirectExecutorService());

        return bot;
    }

    private static AppConfig getConfigFromBot(XBot bot) throws Exception {
        Field f = bot.getClass().getDeclaredField("config");
        f.setAccessible(true);
        return (AppConfig) f.get(bot);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Update updateWithText(Long chatId, Long userId, String firstName, String text) {
        Update u = new Update();
        u.setUpdateId(1);

        Message m = baseMessage(chatId, userId, firstName);
        m.setText(text);

        u.setMessage(m);
        return u;
    }

    private static Update updateWithDocument(Long chatId, Long userId, String firstName, String fileName, Long fileSize) {
        Update u = new Update();
        u.setUpdateId(1);

        Message m = baseMessage(chatId, userId, firstName);

        Document d = new Document();
        d.setFileName(fileName);
        d.setFileSize(fileSize);
        d.setFileId("FILE_ID");
        m.setDocument(d);

        u.setMessage(m);
        return u;
    }

    private static Message baseMessage(Long chatId, Long userId, String firstName) {
        Message m = new Message();
        m.setMessageId(1);

        Chat chat = new Chat(chatId, "channel");
        m.setChat(chat);

        User from = new User(userId, firstName, false);
        m.setFrom(from);

        return m;
    }

    static class DirectExecutorService extends AbstractExecutorService {
        private volatile boolean shutdown;

        @Override public void shutdown() { shutdown = true; }

        @Override public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override public boolean isShutdown() { return shutdown; }

        @Override public boolean isTerminated() { return shutdown; }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            if (shutdown) throw new IllegalStateException("Executor is shutdown");
            command.run();
        }
    }
}
