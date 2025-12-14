package com.xbot.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageInvalidJsonTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @ParameterizedTest
    @ValueSource(strings = {
            "{",                         // обрезанный JSON
            "{\"id\":\"oops\"}"         // id должен быть long
    })
    void invalidJson_throws(String badJson) {
        assertThrows(Exception.class, () -> mapper.readValue(badJson, ChatMessage.class));
    }
}
