package com.xbot.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.xbot.model.ChatExport;
import com.xbot.model.ChatMessage;
import com.xbot.model.ExtractionResult;
import com.xbot.model.TextEntity;
import com.xbot.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

            // Map to cross-reference: username (lowercase) -> telegramId (user_id)
            Map<String, String> usernameToId = new HashMap<>();

            // First pass: collect participants and build username->id mapping from text_entities
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

                // Extract mentions from text_entities (more reliable - includes user_id)
                if (msg.textEntities() != null) {
                    for (TextEntity entity : msg.textEntities()) {
                        if (entity.isMention()) {
                            String effectiveUserId = entity.getEffectiveUserId();
                            String text = entity.text();

                            if (effectiveUserId != null) {
                                // We have user_id - use it for deduplication
                                String username = text != null && text.startsWith("@")
                                        ? text.substring(1)
                                        : null;
                                // Use display name without @ prefix
                                String displayName = text != null && text.startsWith("@")
                                        ? text.substring(1)
                                        : text;
                                mentions.add(new User(effectiveUserId, username, displayName, displayName));

                                // Also store the mapping for cross-referencing
                                if (username != null) {
                                    usernameToId.put(username.toLowerCase(), effectiveUserId);
                                }
                            } else if ("mention".equals(entity.type()) && text != null && text.startsWith("@")) {
                                // @username mention without user_id - will try to cross-reference later
                                String username = text.substring(1);
                                // Check if we already know this user's ID
                                String knownId = usernameToId.get(username.toLowerCase());
                                if (knownId != null) {
                                    mentions.add(new User(knownId, username, username, username));
                                } else {
                                    mentions.add(new User(username, username, username, username));
                                }
                            }
                        }
                    }
                }

                // Note: We only extract mentions from text_entities (above), not from raw text.
                // Regex extraction from raw text causes false positives like @Override (Java annotations).

                if (msg.action() != null && msg.action().contains("channel")) {
                    channels.add(new User(msg.actor() != null ? msg.actor() : "unknown"));
                }
            }

            // Second pass: filter out mentions that match participants by normalized name
            Map<String, User> participantsByNormalizedName = new HashMap<>();
            for (User p : participants) {
                if (p.name() != null) {
                    participantsByNormalizedName.put(normalizeName(p.name()), p);
                }
            }

            Set<User> filteredMentions = new HashSet<>();
            for (User mention : mentions) {
                String normalizedUsername = normalizeName(mention.username() != null ? mention.username() : mention.telegramId());
                if (!participantsByNormalizedName.containsKey(normalizedUsername)) {
                    filteredMentions.add(mention);
                }
            }

            return new ExtractionResult(participants, filteredMentions, channels);

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

    /**
     * Normalizes a name for comparison: lowercase, remove spaces and underscores.
     */
    private String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("[\\s_]", "");
    }
}