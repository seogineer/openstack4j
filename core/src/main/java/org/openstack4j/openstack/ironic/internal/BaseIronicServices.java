package org.openstack4j.openstack.ironic.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.ironic.BaremetalPatch;
import org.openstack4j.model.ironic.options.BaremetalAttributes;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.ironic.domain.PatchBody;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.openstack.common.functions.EnforceVersionToURL;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.PathSegment;
import org.openstack4j.openstack.internal.microversion.JsonBody;

/**
 * Base Ironic service layer. Adds the bare metal microversion header when the session turned microversions on.
 * A 404 from a list, create, update or a call that returns a model is raised; single gets return null for a
 * missing resource and ActionResponse calls report a failed response.
 */
public class BaseIronicServices extends BaseOpenStackService {

    private static final ObjectMapper PLAIN = new ObjectMapper();
    private static final Set<HttpMethod> PASSTHRU_METHODS = EnumSet.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);
    private static final Set<HttpMethod> BODY_METHODS = EnumSet.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    public BaseIronicServices() {
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
            IronicMicroVersions.SUPPORT.headers(version).forEach(invocation::header);
        return invocation;
    }

    /** @return the microversion a request with {@code ceiling} would carry, or {@code null} when microversions are off */
    protected MicroVersion effectiveMicroVersion(MicroVersion ceiling) {
        return IronicMicroVersions.SUPPORT.effective(classCeiling(), ceiling);
    }

    protected boolean isMicroVersionAtLeast(MicroVersion version) {
        MicroVersion effective = effectiveMicroVersion(null);
        return effective != null && effective.compareTo(version) >= 0;
    }

    public static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }

    /** @return the id as an encoded path segment */
    protected static String id(String value) {
        return PathSegment.encode(value);
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

    /** {@code POST <resource>/vendor_passthru?method=} with {@code args} (drivers and nodes share it); the response body is dropped. */
    protected ActionResponse passthruAction(String resourcePath, String method, Map<String, ?> args) {
        return postWithResponse(resourcePath + "/vendor_passthru").param("method", PathSegment.validate(method))
                .entity(JsonBody.of(args == null ? Map.of() : args)).execute();
    }

    /**
     * A vendor passthru call that returns the result. Only POST, PUT and PATCH send a body; GET and DELETE reject non-empty
     * {@code args}. A missing resource (404) raises.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Map<String, Object> passthru(String resourcePath, HttpMethod httpMethod, String method, Map<String, ?> args) {
        Objects.requireNonNull(httpMethod, "httpMethod");
        if (!PASSTHRU_METHODS.contains(httpMethod))
            throw new IllegalArgumentException("vendor passthru does not take " + httpMethod);
        boolean withBody = BODY_METHODS.contains(httpMethod);
        if (!withBody && args != null && !args.isEmpty())
            throw new IllegalArgumentException(httpMethod + " vendor passthru sends no body; args must be empty");
        Invocation<Map> invocation = request(httpMethod, Map.class, resourcePath + "/vendor_passthru").param("method", PathSegment.validate(method));
        if (withBody)
            invocation.entity(JsonBody.of(args == null ? Map.of() : args));
        return (Map<String, Object>) invocation.execute((ExecutionOptions) ExecutionOptions.create(BaseIronicServices::passthruResult, PropagateOnStatus.on(404)));
    }

    /** Empty body gives null, a JSON object a Map, JSON null null, any other JSON {@code {"return": value}}; a non-JSON body (attach) raises. */
    static Map<String, Object> passthruResult(HttpResponse response) {
        if (response.getStatus() == 204)
            return null;
        byte[] bytes;
        try (InputStream in = response.getInputStream()) {
            bytes = in == null ? new byte[0] : in.readAllBytes();
        } catch (IOException e) {
            throw new ClientResponseException(e.getMessage(), 0, e);
        }
        if (bytes.length == 0)
            return null;
        JsonNode node;
        try {
            node = PLAIN.readTree(bytes);
        } catch (IOException e) {
            throw new ClientResponseException("vendor passthru answered a non-JSON body (attach methods are not supported)", response.getStatus(), e);
        }
        if (node == null || node.isNull() || node.isMissingNode())
            return null;
        if (node.isObject())
            return PLAIN.convertValue(node, new TypeReference<LinkedHashMap<String, Object>>() {});
        Map<String, Object> wrapped = new LinkedHashMap<>();
        wrapped.put("return", PLAIN.convertValue(node, Object.class));
        return wrapped;
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
