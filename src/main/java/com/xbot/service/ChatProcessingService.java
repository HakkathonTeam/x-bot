package com.xbot.service;

import com.xbot.model.ChatMessage;
import com.xbot.model.ExtractionResult;
import com.xbot.model.User;
import com.xbot.parser.ChatHistoryParser;
import com.xbot.parser.JsonChatParser;
import com.xbot.parser.ParserFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Тестовый сервис для проверки JsonChatParser
 * TODO: удалить после тестирования
 */
public class ChatProcessingService {

    private final ExcelGenerator excelGenerator = new ExcelGenerator();

    /**
     * Магический метод
     *
     * @param chatFiles список файлов с чатами (JSON/HTML)
     * @param chatName имя чата (для Excel)
     * @return путь к Excel файлу
     */
    public String process(List<Path> chatFiles, String chatName) throws Exception {

        List<User> allUsers = new ArrayList<>();

        for (Path file : chatFiles) {
            String content = Files.readString(file);

            ChatHistoryParser parser = ParserFactory.getParser(content);
            ExtractionResult chat = parser.parse(content);

            Set<User> participants = new HashSet<>();
            Set<String> mentions   = new HashSet<>();
            Set<String> channels   = new HashSet<>();

            if (parser instanceof JsonChatParser jsonParser) {
                List<ChatMessage> messages = chat.messages() != null ? chat.messages() : List.of();
                jsonParser.extractFromMessages(messages, participants, mentions, channels);
            }

            allUsers.addAll(participants);
        }

        String tempDir = System.getProperty("java.io.tmpdir");

        return excelGenerator
                .generateUsersExcel(allUsers, chatName, tempDir)
                .getAbsolutePath();
    }
}