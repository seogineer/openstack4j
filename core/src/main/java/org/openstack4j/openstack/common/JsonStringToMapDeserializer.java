package org.openstack4j.openstack.common;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads a JSON object that the server may send either inline or JSON-encoded in a string (Mistral {@code jsontype}
 * fields). A string that is empty, {@code "null"}, not a JSON object or not valid JSON gives {@code null}, so one odd
 * row does not fail a whole list.
 */
public class JsonStringToMapDeserializer extends JsonDeserializer<Map<String, Object>> {

    private static final ObjectMapper PLAIN = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<LinkedHashMap<String, Object>>() {
    };

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        switch (parser.currentToken()) {
            case START_OBJECT:
                return (Map<String, Object>) context.readValue(parser, context.getTypeFactory().constructType(MAP));
            case VALUE_STRING:
                return parse(parser.getText());
            default:
                return (Map<String, Object>) context.handleUnexpectedToken(Map.class, parser);
        }
    }

    private static Map<String, Object> parse(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty())
            return null;
        try {
            JsonNode node = PLAIN.readTree(trimmed);
            return node != null && node.isObject() ? PLAIN.convertValue(node, MAP) : null;
        } catch (IOException e) {
            return null;
        }
    }
}
