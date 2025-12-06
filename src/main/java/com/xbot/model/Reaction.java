package com.xbot.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Reaction(
        @JsonProperty("type") String type,
        @JsonProperty("count") int count,
        @JsonProperty("emoji") String emoji,
        @JsonProperty("recent") List<ReactionUser> recent
) {}