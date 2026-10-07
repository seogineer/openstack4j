package org.openstack4j.openstack.baremetal.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.options.BaremetalAttributes;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.baremetal.domain.PatchBody;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.common.functions.EnforceVersionToURL;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.JsonBody;

/**
 * Base Ironic service layer. Adds the bare metal microversion header when the session turned microversions on.
 * A 404 from a list, create, update or a call that returns a model is raised; single gets return null for a
 * missing resource and ActionResponse calls report a failed response.
 */
public class BaseBaremetalServices extends BaseOpenStackService {

    public BaseBaremetalServices() {
        super(ServiceType.BAREMETAL, EnforceVersionToURL.instance("/v1"));
    }

    /** Highest microversion every API of this service supports, or {@code null} for no limit. */
    protected MicroVersion classCeiling() {
        return null;
    }

    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        return capped(invocation, null);
    }

    /** Sends this request at no more than {@code ceiling}. */
    protected <R> Invocation<R> capped(Invocation<R> invocation, MicroVersion ceiling) {
        MicroVersion version = effectiveMicroVersion(ceiling);
        if (version != null)
            BaremetalMicroVersions.SUPPORT.headers(version).forEach(invocation::header);
        return invocation;
    }

    /** @return the microversion a request with {@code ceiling} would carry, or {@code null} when microversions are off */
    protected MicroVersion effectiveMicroVersion(MicroVersion ceiling) {
        return BaremetalMicroVersions.SUPPORT.effective(classCeiling(), ceiling);
    }

    protected boolean isMicroVersionAtLeast(MicroVersion version) {
        MicroVersion effective = effectiveMicroVersion(null);
        return effective != null && effective.compareTo(version) >= 0;
    }

    /** Fails before any request when {@code feature} needs a microversion the session does not send. */
    protected void requireMicroVersion(String feature, MicroVersion floor) {
        BaremetalMicroVersions.SUPPORT.require(feature, floor, effectiveMicroVersion(null));
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

    protected <E> E showStrict(Class<E> type, String path) {
        return get(type, path).execute(propagate404());
    }

    /** Ironic create bodies have no root element. */
    protected <E> E create(Class<E> type, String path, BaremetalAttributes<?> attributes) {
        return post(type, path).entity(JsonBody.of(Objects.requireNonNull(attributes, "attributes").toMap())).execute(propagate404());
    }

    protected <E> E patchWith(Class<E> type, String path, List<BaremetalPatch> patches) {
        return patch(type, path).entity(PatchBody.of(patches)).execute(propagate404());
    }

    protected ActionResponse remove(String path) {
        return deleteWithResponse(path).execute();
    }

    /** A {@code PUT} whose response has no body (202 or 204). A 4xx is a failed response, a 5xx raises. */
    protected ActionResponse action(String path, Map<String, ?> body) {
        return putWithResponse(path).entity(JsonBody.of(body)).execute();
    }

    /**
     * @return {@code filters} plus {@code detail=true}, unless the caller set {@code detail} or {@code fields}
     *         (Ironic rejects {@code detail} together with {@code fields})
     */
    protected static Map<String, String> withDetail(Map<String, String> filters) {
        Map<String, String> query = filters == null ? new HashMap<>() : new HashMap<>(filters);
        if (!query.containsKey("fields"))
            query.putIfAbsent("detail", "true");
        return query;
    }
}
