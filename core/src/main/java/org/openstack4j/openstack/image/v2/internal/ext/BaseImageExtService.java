package org.openstack4j.openstack.image.v2.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.v2.options.ImageAttributes;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.image.v2.internal.BaseImageServices;
import org.openstack4j.openstack.internal.microversion.JsonBody;

/**
 * Shared helpers of the Glance extension services. A 404 from a list, create, update or action means the API is
 * disabled (older server, cache middleware off) or the parent is missing, so it is raised; single gets keep
 * returning null for a missing resource.
 */
public abstract class BaseImageExtService extends BaseImageServices {

    public static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }

    protected static String id(String value) {
        return Objects.requireNonNull(value, "id");
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

    protected <E> E create(Class<E> type, String path, ImageAttributes<?> attributes) {
        return post(type, path).entity(JsonBody.of(Objects.requireNonNull(attributes).toMap())).execute(propagate404());
    }

    protected <E> E replace(Class<E> type, String path, ImageAttributes<?> attributes) {
        return put(type, path).entity(JsonBody.of(Objects.requireNonNull(attributes).toMap())).execute(propagate404());
    }

    protected ActionResponse remove(String path) {
        return deleteWithResponse(path).execute();
    }
}
