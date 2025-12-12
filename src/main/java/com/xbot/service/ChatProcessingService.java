package com.xbot.service;

import com.xbot.model.User;
import com.xbot.model.ExtractionResult;
import com.xbot.parser.ChatHistoryParser;
import com.xbot.parser.ParserFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Сервис для проверки JsonChatParser
 */
public class ChatProcessingService {

    private final ExcelGenerator excelGenerator;

    public ChatProcessingService() {
        this.excelGenerator = new ExcelGenerator();
    }

    /**
     * Магический метод
     *
     * @param chatFiles список файлов с чатами (JSON/HTML)
     * @param chatName имя чата (для Excel)
     * @param tempDirectory временная директория для генерации Excel
     * @return путь к Excel файлу
     */
    public String process(List<Path> chatFiles, String chatName, String tempDirectory) throws Exception {

        Set<User> allParticipants = new HashSet<>();
        Set<User> allMentions = new HashSet<>();
        Set<User> allChannels = new HashSet<>();

        for (Path file : chatFiles) {
            if (!Files.exists(file)) {
                System.err.println("Файл не найден: " + file);
                continue;
            }

            String content = Files.readString(file);

            // Выбираем нужный парсер
            ChatHistoryParser parser = ParserFactory.getParser(content);

            // Получаем ExtractionResult
            ExtractionResult result = parser.parse(content);

            // Добавляем участников, mentions и channels
            if (result.participants() != null) allParticipants.addAll(result.participants());
            if (result.mentions() != null) allMentions.addAll(result.mentions());
            if (result.channels() != null) allChannels.addAll(result.channels());
        }

        // Объединяем все в одну коллекцию для Excel
        allParticipants.addAll(allMentions);
        allParticipants.addAll(allChannels);

        return excelGenerator.generateUsersExcel(
                new ArrayList<>(allParticipants),
                chatName,
                tempDirectory
        ).getAbsolutePath();
    }
}