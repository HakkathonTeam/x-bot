package com.xbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Result of extracting users from chat history.
 * Contains separate collections for participants, mentions, and channels.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractionResult(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("id") long id,
        @JsonProperty("messages") List<ChatMessage> messages
) {}