package com.xbot.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.xbot.model.ChatMessage;
import com.xbot.model.ExtractionResult;
import com.xbot.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Telegram JSON chat exports.
 * TODO: Implement by Alexey
 */
public class JsonChatParser implements ChatHistoryParser {

    private final ObjectMapper mapper;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\w+");

    public JsonChatParser() {
        mapper = new ObjectMapper();
        // Подключаем модуль для работы с LocalDateTime
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public ExtractionResult parse(String content) throws ParserException {
        if (content == null || content.isBlank()) {
            throw new ParserException("Empty content");
        }

        try {
            // Десериализация JSON в ExtractionResult
            return mapper.readValue(content, ExtractionResult.class);
        } catch (JsonProcessingException e) {
            throw new ParserException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean canParse(String content) {
        if (content == null || content.isBlank()) return false;

        try {
            JsonNode root = new ObjectMapper().readTree(content);
            return root.isObject() && root.has("messages") && root.get("messages").isArray();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getFormatName() {
        return "JSON";
    }

    /**
     * Извлекаем участников, упоминания и каналы из списка сообщений
     */
    public void extractFromMessages(List<ChatMessage> messages,
                                    Set<User> participants,
                                    Set<String> mentions,
                                    Set<String> channels) {
        if (messages == null) return;

        for (ChatMessage msg : messages) {
            // Участники
            if (msg.from() != null) {
                participants.add(new User(msg.from(), "", "", "", msg.fromId(), msg.from()));
            }

            // Упоминания
            extractMentions(msg.text(), mentions);

            // Каналы (service messages)
            if ("service".equals(msg.type()) && msg.actor() != null) {
                channels.add(msg.actor());
            }
        }
    }

    /**
     * Извлекаем упоминания из поля text
     */
    @SuppressWarnings("unchecked")
    private void extractMentions(Object textField, Set<String> mentions) {
        if (textField == null) return;

        if (textField instanceof String str) {
            Matcher m = MENTION_PATTERN.matcher(str);
            while (m.find()) mentions.add(m.group());
        } else if (textField instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String strItem) {
                    Matcher m = MENTION_PATTERN.matcher(strItem);
                    while (m.find()) mentions.add(m.group());
                } else if (item instanceof java.util.Map<?, ?> mapItem) {
                    Object text = mapItem.get("text");
                    if (text instanceof String strMap) {
                        Matcher m = MENTION_PATTERN.matcher(strMap);
                        while (m.find()) mentions.add(m.group());
                    }
                }
            }
        }
    }
}