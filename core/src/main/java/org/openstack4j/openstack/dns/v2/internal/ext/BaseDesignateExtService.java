package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.options.DesignateAttributes;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.dns.v2.internal.BaseDNSServices;
import org.openstack4j.openstack.internal.microversion.JsonBody;

/**
 * Shared helpers of the Designate extension services. A 404 from a list, create, update or a call that returns a
 * model is raised; single gets return null for a missing resource and ActionResponse calls report a failed response.
 */
public abstract class BaseDesignateExtService extends BaseDNSServices {

    public static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }

    /** @return {@code value}, which must be a non-blank path segment (no {@code /}, {@code ?} or {@code #}) */
    protected static String id(String value) {
        return validId(value);
    }

    /** @return {@code value}, which must be a non-blank path segment (no {@code /}, {@code ?} or {@code #}) */
    public static String validId(String value) {
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

    protected <E> E showStrict(Class<E> type, String path) {
        return get(type, path).execute(propagate404());
    }

    /** Designate bodies have no root element. */
    protected <E> E create(Class<E> type, String path, DesignateAttributes<?> attributes) {
        return post(type, path).entity(JsonBody.of(Objects.requireNonNull(attributes, "attributes").toMap())).execute(propagate404());
    }

    /** A {@code PATCH} with the fields to change. */
    protected <E> E update(Class<E> type, String path, DesignateAttributes<?> attributes) {
        return patch(type, path).entity(JsonBody.of(Objects.requireNonNull(attributes, "attributes").toMap())).execute(propagate404());
    }

    /** A {@code POST} whose response body is not needed. A 4xx or 5xx is a failed response. */
    protected ActionResponse action(String path, Map<String, ?> body) {
        return postWithResponse(path).entity(JsonBody.of(body)).execute();
    }

    protected ActionResponse remove(String path) {
        return deleteWithResponse(path).execute();
    }
}
