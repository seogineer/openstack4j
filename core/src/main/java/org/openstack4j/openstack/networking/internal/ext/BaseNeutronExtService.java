package org.openstack4j.openstack.networking.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.options.NeutronAttributes;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.networking.internal.BaseNetworkingServices;

/** Shared list/show/create/update/delete helpers for the Neutron extension services. */
public abstract class BaseNeutronExtService extends BaseNetworkingServices {

    protected static String id(String value) {
        return Objects.requireNonNull(value, "id");
    }

    protected <E> List<E> listOf(Class<? extends ListResult<E>> type, String path, Map<String, String> filters) {
        ListResult<E> result = get(type, path).params(filters == null ? Collections.emptyMap() : filters).execute(NeutronExecution.propagate404());
        return result == null ? Collections.emptyList() : result.getList();
    }

    protected <E> E show(Class<E> type, String path) {
        return get(type, path).execute();
    }

    protected <E> E create(Class<E> type, String path, String root, NeutronAttributes<?> attributes) {
        return post(type, path).entity(JsonBody.of(root, Objects.requireNonNull(attributes, "attributes").toMap())).execute(NeutronExecution.propagate404());
    }

    protected <E> E update(Class<E> type, String path, String root, NeutronAttributes<?> attributes) {
        return put(type, path).entity(JsonBody.of(root, Objects.requireNonNull(attributes, "attributes").toMap())).execute(NeutronExecution.propagate404());
    }

    /** Like {@link #show} but a 404 is raised: for list-like resources that have no "missing" meaning. */
    protected <E> E showStrict(Class<E> type, String path) {
        return get(type, path).execute(NeutronExecution.propagate404());
    }

    protected ActionResponse remove(String path) {
        return deleteWithResponse(path).execute();
    }
}
