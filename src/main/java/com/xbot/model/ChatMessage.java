package com.xbot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.xbot.model.serializer.CustomTextDeserializer;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a single message from chat history.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessage(
        @JsonProperty("id") long id,
        @JsonProperty("type") String type,

        @JsonProperty("date")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime date,

        @JsonProperty("date_unixtime") String dateUnixTime,

        @JsonProperty("actor") String actor,
        @JsonProperty("actor_id") String actorId,
        @JsonProperty("action") String action,
        @JsonProperty("members") List<String> members,
        @JsonProperty("from") String from,
        @JsonProperty("from_id") String fromId,

        @JsonProperty("reply_to_message_id") Long replyToMessageId,

        @JsonProperty("photo") String photo,
        @JsonProperty("photo_file_size") Integer photoFileSize,
        @JsonProperty("width") Integer width,
        @JsonProperty("height") Integer height,

        @JsonProperty("edited")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime edited,

        @JsonProperty("edited_unixtime") String editedUnixTime,

        @JsonProperty("text")
        @JsonDeserialize(using = CustomTextDeserializer.class) Object text,
        @JsonProperty("text_entities") List<TextEntity> textEntities,
        @JsonProperty("reactions") List<Reaction> reactions,

        // Дополнительные поля, которые могут быть в экспорте Telegram
        @JsonProperty("inviter") String inviter,
        @JsonProperty("sticker_emoji") String stickerEmoji,
        @JsonProperty("forwarded_from") String forwardedFrom,
        @JsonProperty("via_bot") String viaBot,
        @JsonProperty("duration_seconds") Integer durationSeconds,
        @JsonProperty("message_id") Long messageId,
        @JsonProperty("file") String file,
        @JsonProperty("thumbnail") String thumbnail,
        @JsonProperty("media_type") String mediaType,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("file_size") Long fileSize,
        @JsonProperty("title") String title,
        @JsonProperty("performer") String performer,
        @JsonProperty("game_title") String gameTitle,
        @JsonProperty("game_description") String gameDescription,
        @JsonProperty("game_link") String gameLink,
        @JsonProperty("score") Integer score,
        @JsonProperty("location_information") Object locationInformation,
        @JsonProperty("live_location_period_seconds") Integer liveLocationPeriodSeconds,
        @JsonProperty("poll") Object poll,
        @JsonProperty("contact_information") Object contactInformation,
        @JsonProperty("contact_vcard") String contactVcard,
        @JsonProperty("author") String author,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("sticker_set_name") String stickerSetName,
        @JsonProperty("thumbnail_file_size") Integer thumbnailFileSize,
        @JsonProperty("thumbnail_width") Integer thumbnailWidth,
        @JsonProperty("thumbnail_height") Integer thumbnailHeight,
        @JsonProperty("caption") String caption
) {
    public boolean isServiceMessage() {
        return "service".equals(type);
    }

    public boolean isRegularMessage() {
        return "message".equals(type);
    }

    public boolean hasPhoto() {
        return photo != null;
    }

    public boolean hasReactions() {
        return reactions != null && !reactions.isEmpty();
    }

    public boolean wasInvitedByUser() {
        return inviter != null && !"Group".equals(inviter);
    }

    public boolean wasInvitedByGroup() {
        return "Group".equals(inviter);
    }
}