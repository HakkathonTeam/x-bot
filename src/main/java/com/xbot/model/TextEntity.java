package com.xbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TextEntity(
        @JsonProperty("type") String type,
        @JsonProperty("text") String text,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("href") String href
) {
    /**
     * Checks if this entity is a mention (either @username or text_link to user).
     */
    public boolean isMention() {
        return "mention".equals(type) || isTextLinkToUser();
    }

    /**
     * Checks if this is a text_link pointing to a user (tg://user?id=XXX).
     */
    public boolean isTextLinkToUser() {
        return "text_link".equals(type) && href != null && href.startsWith("tg://user?id=");
    }

    /**
     * Extracts user_id from href for text_link entities.
     * Returns null if not a text_link to user.
     */
    public String extractUserIdFromHref() {
        if (isTextLinkToUser()) {
            return href.replace("tg://user?id=", "");
        }
        return null;
    }

    /**
     * Gets the effective user ID - either from user_id field or extracted from href.
     */
    public String getEffectiveUserId() {
        if (userId != null) {
            return "user" + userId;
        }
        String hrefUserId = extractUserIdFromHref();
        if (hrefUserId != null) {
            return "user" + hrefUserId;
        }
        return null;
    }
}