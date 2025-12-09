package com.xbot.parser;

import com.xbot.service.ChatProcessingService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Тестовый класс для проверки JsonChatParser
 * TODO: удалить после тестирования
 */
public class JsonChatParserFileTest {

    // Пути к JSON-файлам чатов
    private static final Path[] CHAT_FILES_ARRAY = {
            Path.of("src/main/resources/chat1.json"),
            Path.of("src/main/resources/chat2.json")
    };

    private static final String CHAT_NAME = "test_chat";

    public static void main(String[] args) {

        try {
            List<Path> chatFiles = new ArrayList<>();

            for (Path path : CHAT_FILES_ARRAY) {
                if (Files.exists(path)) {
                    chatFiles.add(path);
                } else {
                    System.err.println("Ошибка (файл не найден): " + path);
                }
            }

            if (chatFiles.isEmpty()) {
                System.err.println("Нет файлов для обработки");
                return;
            }

            ChatProcessingService service = new ChatProcessingService();
            String excelPath = service.process(chatFiles, CHAT_NAME);

            System.out.println("Excel файл успешно создан: " + excelPath);

        } catch (Exception e) {
            System.err.println("Ошибка при обработке чата:");
            e.printStackTrace(System.err);
        }
    }
}