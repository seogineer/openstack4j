package org.openstack4j.openstack.reservation.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

/**
 * Base of the Blazar (reservation v1) services. Requests are plain objects, responses are wrapped. A 404 from a list,
 * create, update or a call that returns a model is raised; single gets return null and ActionResponse calls report a
 * failed response.
 */
public abstract class BaseBlazarService extends BaseOpenStackService {

    protected BaseBlazarService() {
        super(ServiceType.RESERVATION);
    }

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

    protected <E> List<E> listOf(Class<? extends ListResult<E>> type, String path, Map<String, String> filters) {
        ListResult<E> result = get(type, path).params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        return result == null ? Collections.emptyList() : result.getList();
    }

    protected <E> E show(Class<E> type, String path) {
        return get(type, path).execute();
    }

    protected <E> E create(Class<E> type, String path, Map<String, ?> body) {
        return post(type, path).entity(JsonBody.of(Objects.requireNonNull(body, "body"))).execute(propagate404());
    }

    protected <E> E update(Class<E> type, String path, Map<String, ?> body) {
        return put(type, path).entity(JsonBody.of(Objects.requireNonNull(body, "body"))).execute(propagate404());
    }

    protected ActionResponse remove(String path) {
        return deleteWithResponse(path).execute();
    }

    /** @return the list of objects under {@code key}; a missing parent raises */
    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> mapsOf(String path, String key, Map<String, String> filters) {
        Map<String, Object> body = get(Map.class, path).params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        Object list = body == null ? null : body.get(key);
        return list instanceof List ? (List<Map<String, Object>>) list : Collections.emptyList();
    }

    /** @return the object under {@code key}; a missing resource raises */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> objectOf(Map<String, Object> body, String key) {
        Object inner = body == null ? null : body.get(key);
        return inner instanceof Map ? (Map<String, Object>) inner : new HashMap<>();
    }
}
