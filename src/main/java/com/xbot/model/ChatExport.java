package com.xbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents raw Telegram JSON export structure.
 * Used for Jackson deserialization of JSON chat exports.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatExport(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("id") long id,
        @JsonProperty("messages") List<ChatMessage> messages
) {}
