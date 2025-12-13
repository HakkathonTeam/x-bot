package com.xbot.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageDeserialization {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Test
    void deserializeTest() throws Exception {
        String asString = "{\"id\":1,\"type\":\"message\",\"date\":\"2025-12-12T10:00:00\",\"date_unixtime\":\"0\",\"text\":\"test\"}";
        ChatMessage m1 = mapper.readValue(asString, ChatMessage.class);
        assertEquals("test", m1.text());

        String asArray = "{\"id\":2,\"type\":\"message\",\"date\":\"2025-12-12T10:00:00\",\"date_unixtime\":\"0\",\"text\":[\"a\",{\"type\":\"bold\",\"text\":\"b\"}]}";
        ChatMessage m2 = mapper.readValue(asArray, ChatMessage.class);

        assertNotNull(m2.text());
        assertTrue(m2.text() instanceof java.util.List);
        assertEquals(2, ((java.util.List<?>) m2.text()).size());
    }
}
