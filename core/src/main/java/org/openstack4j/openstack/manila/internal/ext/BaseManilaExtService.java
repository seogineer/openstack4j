package org.openstack4j.openstack.manila.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.options.ManilaAttributes;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;

/**
 * Base of the Manila services added in 4.6. Each request carries a microversion: the session's when microversions are
 * on (it must be at least the method's floor), otherwise the method's floor. A 404 from a list, create, update or a
 * call that returns a model is raised; single gets return null and ActionResponse calls report a failed response.
 */
public abstract class BaseManilaExtService extends BaseOpenStackService {

    protected BaseManilaExtService() {
        super(ServiceType.SHARE, ManilaMicroVersions::v2Url);
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

    /** @return the microversion a request with this floor carries */
    protected static MicroVersion versionFor(String feature, MicroVersion floor) {
        MicroVersion effective = ManilaMicroVersions.SUPPORT.effective(null, null);
        if (effective == null)
            return floor;
        ManilaMicroVersions.SUPPORT.require(feature, floor, effective);
        return effective;
    }

    /** Adds the microversion headers for a method with {@code floor}; fails before the request when the session is lower. */
    protected <R> Invocation<R> at(MicroVersion floor, Invocation<R> invocation, String feature) {
        ManilaMicroVersions.SUPPORT.headers(versionFor(feature, floor)).forEach(invocation::header);
        return invocation;
    }

    protected <E> List<E> listOf(MicroVersion floor, Class<? extends ListResult<E>> type, String path, Map<String, String> filters) {
        ListResult<E> result = at(floor, get(type, path), path).params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        return result == null ? Collections.emptyList() : result.getList();
    }

    protected <E> E show(MicroVersion floor, Class<E> type, String path) {
        return at(floor, get(type, path), path).execute();
    }

    protected <E> E showStrict(MicroVersion floor, Class<E> type, String path) {
        return at(floor, get(type, path), path).execute(propagate404());
    }

    /** {@code POST} with {@code {"<root>": {fields}}}. */
    protected <E> E create(MicroVersion floor, Class<E> type, String path, String root, ManilaAttributes<?> attributes) {
        return at(floor, post(type, path), path).entity(JsonBody.of(root, Objects.requireNonNull(attributes, "attributes").toMap())).execute(propagate404());
    }

    /** {@code PUT} with {@code {"<root>": {fields}}}. */
    protected <E> E update(MicroVersion floor, Class<E> type, String path, String root, ManilaAttributes<?> attributes) {
        return at(floor, put(type, path), path).entity(JsonBody.of(root, Objects.requireNonNull(attributes, "attributes").toMap())).execute(propagate404());
    }

    /** {@code POST <path>/action} with {@code {"<name>": body}}; {@code body} {@code null} sends {@code {"<name>": null}}. */
    protected ActionResponse action(MicroVersion floor, String path, String name, Map<String, ?> body) {
        Map<String, Object> wrapper = new java.util.HashMap<>();
        wrapper.put(name, body);
        return at(floor, postWithResponse(path + "/action"), path + "/action " + name).entity(JsonBody.of(wrapper)).execute();
    }

    /** A {@code POST} or {@code PUT} with a plain body whose response body is not needed. */
    protected ActionResponse send(MicroVersion floor, Invocation<ActionResponse> invocation, String feature, Map<String, ?> body) {
        return at(floor, invocation, feature).entity(JsonBody.of(body)).execute();
    }

    protected ActionResponse remove(MicroVersion floor, String path) {
        return at(floor, deleteWithResponse(path), path).execute();
    }

    /** Adds {@code X-OpenStack-Manila-API-Experimental: True} when the request's microversion is below {@code stableFrom}. */
    protected <R> Invocation<R> experimentalHeader(MicroVersion floor, MicroVersion stableFrom, Invocation<R> invocation, String feature) {
        if (versionFor(feature, floor).compareTo(stableFrom) < 0)
            invocation.header("X-OpenStack-Manila-API-Experimental", "True");
        return invocation;
    }

    /** An action of an API that was experimental below {@code stableFrom}. */
    protected ActionResponse experimental(MicroVersion floor, MicroVersion stableFrom, String path, String name, Map<String, ?> body) {
        Map<String, Object> wrapper = new java.util.HashMap<>();
        wrapper.put(name, body);
        String feature = path + "/action " + name;
        return experimentalHeader(floor, stableFrom, at(floor, postWithResponse(path + "/action"), feature), feature).entity(JsonBody.of(wrapper)).execute();
    }

    /** @return {@code GET <path>} unwrapped from {@code {"metadata": {...}}}; a missing resource raises */
    protected Map<String, String> metadataOf(MicroVersion floor, String path) {
        return strings(showStrict(floor, Map.class, path), "metadata");
    }

    /** @return one item of {@code GET <path>/<key>} ({@code {"meta": {k: v}}} or {@code {"metadata": {k: v}}}), or {@code null} */
    protected String metadataItem(MicroVersion floor, String path, String key) {
        Map<?, ?> body = show(floor, Map.class, path + "/" + id(key));
        Map<String, String> meta = strings(body, "meta");
        return meta.isEmpty() ? strings(body, "metadata").get(key) : meta.get(key);
    }

    /** {@code POST} (merge, {@code replace} false) or {@code PUT} (replace all) {@code {"metadata": {...}}}; @return all items */
    protected Map<String, String> writeMetadata(MicroVersion floor, String path, Map<String, String> metadata, boolean replace) {
        Invocation<Map> invocation = replace ? put(Map.class, path) : post(Map.class, path);
        return strings(at(floor, invocation, path).entity(JsonBody.of("metadata", Objects.requireNonNull(metadata, "metadata"))).execute(propagate404()), "metadata");
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, String> strings(Map<?, ?> body, String root) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        Object inner = body == null ? null : body.get(root);
        if (inner instanceof Map)
            ((Map<String, Object>) inner).forEach((k, v) -> result.put(k, v == null ? null : String.valueOf(v)));
        return result;
    }
}
