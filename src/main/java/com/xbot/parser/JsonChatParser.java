package com.xbot.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.xbot.model.ChatExport;
import com.xbot.model.ChatMessage;
import com.xbot.model.ExtractionResult;
import com.xbot.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xbot.service.MentionExtractor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parser for Telegram JSON chat exports.
 * TODO: Implement by Alexey
 */
public class JsonChatParser implements ChatHistoryParser {

    private final ObjectMapper mapper;
    private final MentionExtractor mentionExtractor;

    public JsonChatParser() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mentionExtractor = new MentionExtractor();
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
                    participants.add(new User(
                            msg.fromId() != null ? msg.fromId() : msg.from(),
                            msg.from(),
                            msg.from()
                    ));
                }

                Object textField = msg.text();
                if (textField != null) {
                    for (String username : mentionExtractor.extract(textField.toString())) {
                        mentions.add(new User(username));
                    }
                }

                if (msg.action() != null && msg.action().contains("channel")) {
                    channels.add(new User(msg.actor() != null ? msg.actor() : "unknown"));
                }
            }

            return new ExtractionResult(
                    participants,
                    mentions,
                    channels
            );

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