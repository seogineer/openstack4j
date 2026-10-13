package org.openstack4j.test.common;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.openstack4j.openstack.common.JsonStringToMapDeserializer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Mistral {@code jsontype} fields come as a JSON object or as a JSON-encoded string.
 */
@Test(suiteName = "JsonStringToMapDeserializer")
public class JsonStringToMapDeserializerTest {

    public static class Holder {
        @JsonProperty("v")
        @JsonDeserialize(using = JsonStringToMapDeserializer.class)
        public Map<String, Object> value;
    }

    private static Map<String, Object> read(String json) throws Exception {
        return new ObjectMapper().readValue(json, Holder.class).value;
    }

    public void inlineObject() throws Exception {
        assertEquals(read("{\"v\": {\"k\": \"v\", \"n\": 1}}"), Map.of("k", "v", "n", 1));
    }

    public void objectInString() throws Exception {
        assertEquals(read("{\"v\": \"{\\\"k\\\": \\\"v\\\"}\"}"), Map.of("k", "v"));
    }

    public void nestedObjectInString() throws Exception {
        Map<String, Object> value = read("{\"v\": \"{\\\"a\\\": {\\\"b\\\": [1, 2]}}\"}");
        assertTrue(value.get("a") instanceof Map);
        assertEquals(((Map<?, ?>) value.get("a")).get("b"), java.util.List.of(1, 2));
    }

    public void emptyAndNullBecomeNull() throws Exception {
        assertNull(read("{\"v\": \"\"}"));
        assertNull(read("{\"v\": \"  \"}"));
        assertNull(read("{\"v\": \"null\"}"));
        assertNull(read("{\"v\": null}"));
    }

    public void whatIsNotAnObjectBecomesNull() throws Exception {
        assertNull(read("{\"v\": \"[1]\"}"));
        assertNull(read("{\"v\": \"5\"}"));
        assertNull(read("{\"v\": \"{broken\"}"));
    }

    public void emptyObject() throws Exception {
        assertEquals(read("{\"v\": {}}"), Map.of());
        assertEquals(read("{\"v\": \"{}\"}"), Map.of());
    }
}
