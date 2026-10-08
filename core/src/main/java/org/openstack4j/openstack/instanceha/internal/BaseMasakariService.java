package org.openstack4j.openstack.instanceha.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.instanceha.options.MasakariAttributes;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

/**
 * Base of the Masakari (instance HA v1) services. A 404 from a list, create, update or a call that returns a model is
 * raised; single gets return null and ActionResponse calls report a failed response.
 */
public abstract class BaseMasakariService extends BaseOpenStackService {

    static final String API_VERSION = "OpenStack-API-Version";

    protected BaseMasakariService() {
        super(ServiceType.INSTANCE_HA);
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

    /** {@code POST} with {@code {"<root>": {fields}}}; the segment {@code enabled} field needs instance-ha 1.2. */
    protected <E> E create(Class<E> type, String path, String root, MasakariAttributes<?> attributes) {
        Map<String, Object> fields = Objects.requireNonNull(attributes, "attributes").toMap();
        return versioned(post(type, path), fields).entity(JsonBody.of(root, fields)).execute(propagate404());
    }

    /** {@code PUT} with {@code {"<root>": {fields}}}. */
    protected <E> E update(Class<E> type, String path, String root, MasakariAttributes<?> attributes) {
        Map<String, Object> fields = Objects.requireNonNull(attributes, "attributes").toMap();
        return versioned(put(type, path), fields).entity(JsonBody.of(root, fields)).execute(propagate404());
    }

    private static <R> Invocation<R> versioned(Invocation<R> invocation, Map<String, Object> fields) {
        if (fields.containsKey("enabled"))
            invocation.header(API_VERSION, "instance-ha 1.2");
        return invocation;
    }

    protected ActionResponse remove(String path) {
        return deleteWithResponse(path).execute();
    }
}
