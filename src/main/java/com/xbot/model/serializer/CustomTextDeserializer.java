package com.xbot.model.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomTextDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken currentToken = p.currentToken();

        if (currentToken == JsonToken.VALUE_STRING) {
            return p.getText();
        } else if (currentToken == JsonToken.START_ARRAY) {
            JsonNode node = p.getCodec().readTree(p);
            if (node.isArray()) {
                List<Object> result = new ArrayList<>();
                for (JsonNode item : node) {
                    if (item.isTextual()) {
                        result.add(item.asText());
                    } else if (item.isObject()) {
                        // Конвертируем объект в Map
                        ObjectMapper mapper = (ObjectMapper) p.getCodec();
                        Map<String, Object> map = mapper.convertValue(item, Map.class);
                        result.add(map);
                    }
                }
                return result;
            }
        }

        return null;
    }
}