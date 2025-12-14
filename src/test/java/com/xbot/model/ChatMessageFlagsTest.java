package com.xbot.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xbot.model.ChatMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageFlagsTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource({
            // name | json | regular | service | photo | reactions | invitedByGroup | invitedByUser

            "regular message," +
                    "'{\"id\":1,\"type\":\"message\",\"date\":\"2025-12-12T10:00:00\",\"date_unixtime\":\"0\",\"from\":\"Tester\",\"from_id\":\"u1\",\"text\":\"hi\"}'," +
                    "true,false,false,false,false,false",

            "service message," +
                    "'{\"id\":2,\"type\":\"service\",\"date\":\"2025-12-12T10:00:00\",\"date_unixtime\":\"0\",\"text\":\"joined\"}'," +
                    "false,true,false,false,false,false",

            "message with photo," +
                    "'{\"id\":3,\"type\":\"message\",\"date\":\"2025-12-12T10:00:00\",\"date_unixtime\":\"0\",\"photo\":\"p.jpg\",\"text\":\"\"}'," +
                    "true,false,true,false,false,false",

            "message with reactions," +
                    "'{\"id\":4,\"type\":\"message\",\"date\":\"2025-12-12T10:00:00\",\"date_unixtime\":\"0\",\"text\":\"ok\",\"reactions\":[{\"emoji\":\"👍\"}]}' ," +
                    "true,false,false,true,false,false",

            "invited by group," +
                    "'{\"id\":5,\"type\":\"message\",\"date\":\"2025-12-12T10:00:00\",\"date_unixtime\":\"0\",\"text\":\"join\",\"inviter\":\"Group\"}'," +
                    "true,false,false,false,true,false",

            "invited by user," +
                    "'{\"id\":6,\"type\":\"message\",\"date\":\"2025-12-12T10:00:00\",\"date_unixtime\":\"0\",\"text\":\"join\",\"inviter\":\"Alice\"}'," +
                    "true,false,false,false,false,true"
    })
    void chatMessageFlags(
            String name,
            String json,
            boolean regular,
            boolean service,
            boolean photo,
            boolean reactions,
            boolean invitedByGroup,
            boolean invitedByUser
    ) throws Exception {

        ChatMessage msg = mapper.readValue(json, ChatMessage.class);

        assertEquals(regular, msg.isRegularMessage(), "isRegularMessage");
        assertEquals(service, msg.isServiceMessage(), "isServiceMessage");
        assertEquals(photo, msg.hasPhoto(), "hasPhoto");
        assertEquals(reactions, msg.hasReactions(), "hasReactions");
        assertEquals(invitedByGroup, msg.wasInvitedByGroup(), "wasInvitedByGroup");
        assertEquals(invitedByUser, msg.wasInvitedByUser(), "wasInvitedByUser");
    }
}
