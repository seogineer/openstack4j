package org.openstack4j.openstack.trove.internal.ext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.trove.internal.BaseTroveServices;

/**
 * Shared helpers of the Trove services added in 4.6. A 404 from a list, create, update or a call that returns a model
 * is raised; single gets return null and ActionResponse calls report a failed response.
 */
public abstract class BaseTroveExtService extends BaseTroveServices {

    public static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }

    /** @return {@code value}, which must be a non-blank path segment (no {@code /}, {@code ?} or {@code #}) */
    protected static String id(String value) {
        Objects.requireNonNull(value, "id");
        if (value.isBlank() || value.indexOf('/') >= 0 || value.indexOf('?') >= 0 || value.indexOf('#') >= 0)
            throw new IllegalArgumentException("Not a valid identifier: '" + value + "'");
        return value;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> mapOf(String path, Map<String, String> filters) {
        return get(Map.class, path).params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
    }

    /** @return the object under {@code key} of {@code GET path}; a missing resource raises */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> objectOf(String path, String key) {
        Map<String, Object> body = mapOf(path, null);
        Object inner = body == null ? null : body.get(key);
        return inner instanceof Map ? (Map<String, Object>) inner : new HashMap<>();
    }

    /** @return the list under {@code key} of {@code GET path}; a missing parent raises */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> listOf(String path, String key, Map<String, String> filters) {
        Map<String, Object> body = mapOf(path, filters);
        Object list = body == null ? null : body.get(key);
        return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
    }

    /** @return the object under {@code key} of the response to {@code POST path} with {@code body}; a missing resource raises */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> postFor(String path, Map<String, ?> body, String key) {
        Map<String, Object> response = post(Map.class, path).entity(JsonBody.of(body)).execute(propagate404());
        Object inner = response == null ? null : response.get(key);
        return inner instanceof Map ? (Map<String, Object>) inner : new HashMap<>();
    }

    /** {@code POST <path>/action} with {@code {"<name>": body}}. */
    protected ActionResponse action(String path, String name, Map<String, ?> body) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(name, body);
        return postWithResponse(path + "/action").entity(JsonBody.of(wrapper)).execute();
    }
}
