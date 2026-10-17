package org.openstack4j.openstack.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.openstack.internal.microversion.JsonBody;

/**
 * Builds the body of an update request from a model object that may have been read from a response: the model is
 * serialised the way it is on create (Jackson annotations, no {@code null}s) into a copy, and the attributes the server
 * only returns and never accepts on update are left out. The caller's object is not touched.
 */
public final class UpdateBody {

    private static final ObjectMapper PLAIN = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private UpdateBody() {
    }

    /**
     * @param root the root element of the request, for example {@code port_pair}
     * @param model the model to send
     * @param readOnlyKeys the JSON names to leave out
     * @return {@code {"<root>": {model minus readOnlyKeys}}}
     */
    @SuppressWarnings("unchecked")
    public static JsonBody of(String root, Object model, String... readOnlyKeys) {
        Map<String, Object> fields = new LinkedHashMap<>(PLAIN.convertValue(model, Map.class));
        for (String key : readOnlyKeys)
            fields.remove(key);
        return JsonBody.of(root, fields);
    }
}
