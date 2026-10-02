package org.openstack4j.openstack.internal.microversion;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.model.ModelEntity;

/**
 * Shared by the microversion-aware services.
 * A request body built from a map that keeps explicit {@code null} values ({@code "key_name": null}), which the
 * client's NON_NULL object mapper would otherwise drop.
 */
public final class JsonBody implements ModelEntity {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper PLAIN = new ObjectMapper();

    private final ObjectNode node;

    private JsonBody(ObjectNode node) {
        this.node = node;
    }

    /** @return {@code {"<root>": {fields}}} */
    public static JsonBody of(String root, Map<String, ?> fields) {
        ObjectNode body = PLAIN.createObjectNode();
        body.set(root, toNode(fields));
        return new JsonBody(body);
    }

    /** @return {@code {fields}} without a root element */
    public static JsonBody of(Map<String, ?> fields) {
        return new JsonBody(toNode(fields));
    }

    private static ObjectNode toNode(Map<String, ?> fields) {
        ObjectNode node = PLAIN.createObjectNode();
        fields.forEach((k, v) -> {
            if (v == null)
                node.putNull(k);
            else
                node.set(k, PLAIN.valueToTree(v));
        });
        return node;
    }

    @JsonValue
    public ObjectNode toJson() {
        return node;
    }
}
