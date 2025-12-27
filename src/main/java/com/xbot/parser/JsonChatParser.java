package com.xbot.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.xbot.model.ChatExport;
import com.xbot.model.ChatMessage;
import com.xbot.model.ExtractionResult;
import com.xbot.model.TextEntity;
import com.xbot.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parser for Telegram JSON chat exports.
 */
public class JsonChatParser implements ChatHistoryParser {

    private final ObjectMapper mapper;

    public JsonChatParser() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public ExtractionResult parse(String content) throws ParserException {
        try {
            if (content == null || content.isBlank()) {
                throw new ParserException("Empty JSON content");
            }

            // Десериализуем JSON в ChatExport
            ChatExport chatExport = mapper.readValue(content, ChatExport.class);

            List<ChatMessage> messages = chatExport.messages() != null ? chatExport.messages() : List.of();

            Set<User> participants = new HashSet<>();
            Set<User> mentions = new HashSet<>();
            Set<User> channels = new HashSet<>();

            for (ChatMessage msg : messages) {
                if (msg.from() != null) {
                    String telegramId = msg.fromId() != null ? msg.fromId() : msg.from();
                    participants.add(new User(
                            telegramId,
                            null,
                            msg.from(),
                            msg.from()
                    ));
                }

                // Extract mentions from text_entities
                if (msg.textEntities() != null) {
                    for (TextEntity entity : msg.textEntities()) {
                        if (entity.isMention()) {
                            String effectiveUserId = entity.getEffectiveUserId();
                            String text = entity.text();
                            String displayName = text != null && text.startsWith("@")
                                    ? text.substring(1)
                                    : text;

                            if (effectiveUserId != null) {
                                mentions.add(new User(effectiveUserId, null, displayName, displayName));
                            } else if ("mention".equals(entity.type()) && text != null && text.startsWith("@")) {
                                String username = text.substring(1);
                                mentions.add(new User(username, username, username, username));
                            }
                        }
                    }
                }

                if (msg.action() != null && msg.action().contains("channel")) {
                    channels.add(new User(msg.actor() != null ? msg.actor() : "unknown"));
                }
            }

            return new ExtractionResult(participants, mentions, channels);

        } catch (Exception e) {
            throw new ParserException("Failed to parse JSON", e);
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
}