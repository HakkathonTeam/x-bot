package com.xbot.parser;

import com.xbot.model.ExtractionResult;
import com.xbot.model.ChatMessage;
import com.xbot.model.User;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Тестовый класс для проверки JsonChatParser
 * TODO: удалить после тестирования
 */
public class JsonChatParserFileTest {

    public static void main(String[] args) {
        System.out.println("Вставьте JSON. В конце напишите END и нажмите Enter");

        try (Scanner scanner = new Scanner(System.in)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while (!(line = scanner.nextLine()).equals("END")) {
                sb.append(line).append("\n");
            }

            String content = sb.toString();

            JsonChatParser parser = new JsonChatParser();
            ExtractionResult result = parser.parse(content);

            Set<User> participants = new HashSet<>();
            Set<String> mentions = new HashSet<>();
            Set<String> channels = new HashSet<>();

            parser.extractFromMessages(result.messages(), participants, mentions, channels);

            System.out.println("\nУчастники:");
            participants.forEach(p -> System.out.println(p.username()));

            System.out.println("\nУпоминания:");
            mentions.forEach(System.out::println);

            System.out.println("\nКаналы:");
            channels.forEach(System.out::println);

        } catch (Exception e) {
            System.err.println("Ошибка парсинга JSON:");
            e.printStackTrace(System.err);
        }
    }
}