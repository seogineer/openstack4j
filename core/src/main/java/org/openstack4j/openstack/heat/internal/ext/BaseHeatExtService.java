package org.openstack4j.openstack.heat.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.heat.options.HeatAttributes;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.heat.internal.BaseHeatServices;

/**
 * Shared helpers of the Heat extension services. A 404 from a list, create, update or a call that returns a
 * model is raised; single gets return null for a missing resource and ActionResponse calls report a failed response.
 */
public abstract class BaseHeatExtService extends BaseHeatServices {

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

    protected <E> E create(Class<E> type, String path, String root, HeatAttributes<?> attributes) {
        return post(type, path).entity(JsonBody.of(root, Objects.requireNonNull(attributes, "attributes").toMap())).execute(propagate404());
    }

    protected <E> E update(Class<E> type, String path, String root, HeatAttributes<?> attributes) {
        return put(type, path).entity(JsonBody.of(root, Objects.requireNonNull(attributes, "attributes").toMap())).execute(propagate404());
    }

    protected ActionResponse remove(String path) {
        return deleteWithResponse(path).execute();
    }
}
