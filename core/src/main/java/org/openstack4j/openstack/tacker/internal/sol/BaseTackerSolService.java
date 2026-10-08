package org.openstack4j.openstack.tacker.internal.sol;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.HttpEntityHandler;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.microversion.JsonBody;

/**
 * Base of the ETSI NFV-SOL services of Tacker. Their paths are rooted at the endpoint (catalogs register
 * {@code http://host:9890/}, older ones {@code .../v1.0}); SOL 013 errors are {@code application/problem+json}.
 */
public abstract class BaseTackerSolService extends BaseOpenStackService {

    private static final Pattern NEXT_MARKER = Pattern.compile("[?&]nextpage_opaque_marker=([^&>;]+)");

    /** The API's root path (e.g. {@code /vnflcm/v2}) and the {@code Version} header it requires, or {@code null}. */
    private final String root;
    private final String version;

    protected BaseTackerSolService(String root, String version) {
        super(ServiceType.TACKER, url -> url.replaceAll("/+$", "").replaceAll("/v1\\.0$", ""));
        this.root = root;
        this.version = version;
    }

    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        return version == null ? invocation : invocation.header("Version", version);
    }

    protected String path(String suffix) {
        return root + suffix;
    }

    protected static String id(String value) {
        Objects.requireNonNull(value, "id");
        if (value.isBlank() || value.indexOf('/') >= 0 || value.indexOf('?') >= 0 || value.indexOf('#') >= 0)
            throw new IllegalArgumentException("Not a valid identifier: '" + value + "'");
        return value;
    }

    protected static Map<String, Object> body(Map<String, ?> body, String name) {
        return new LinkedHashMap<>(Objects.requireNonNull(body, name));
    }

    /** Raises the SOL 013 problem ({@code detail}, else {@code title}) of a failed response. */
    protected static ResponseException failure(HttpResponse response) {
        String message = null;
        try (InputStream in = response.getInputStream()) {
            if (in != null) {
                JsonNode problem = ObjectMapperSingleton.getContext(Map.class).readTree(in);
                if (problem != null && problem.hasNonNull("detail"))
                    message = problem.get("detail").asText();
                else if (problem != null && problem.hasNonNull("title"))
                    message = problem.get("title").asText();
            }
        } catch (IOException | RuntimeException e) {
            // not a problem document
        } finally {
            HttpEntityHandler.closeQuietly(response);
        }
        return message == null ? ResponseException.mapException(response) : ResponseException.mapException(message, response.getStatus());
    }

    /** A page of a SOL list: {@code items}, and {@code next}, the {@code nextpage_opaque_marker} of the next page or {@code null}. */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> page(String path, Map<String, String> params) {
        Invocation<List> invocation = get(List.class, path);
        if (params != null)
            invocation.params(params);
        HttpResponse response = invocation.executeWithResponse();
        if (response.getStatus() >= 400)
            throw failure(response);
        String link = response.header("Link");
        List<Object> items = response.getStatus() == 204 ? null : response.getEntity(List.class);
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("items", items == null ? new ArrayList<>() : items);
        page.put("next", nextMarker(link));
        return page;
    }

    static String nextMarker(String link) {
        if (link == null || !link.contains("rel=\"next\"") && !link.contains("rel=next"))
            return null;
        Matcher m = NEXT_MARKER.matcher(link);
        return m.find() ? URLDecoder.decode(m.group(1), StandardCharsets.UTF_8) : null;
    }

    /** A resource, or {@code null} when it does not exist (404). */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> show(String path) {
        HttpResponse response = get(Map.class, path).executeWithResponse();
        if (response.getStatus() == 404) {
            HttpEntityHandler.closeQuietly(response);
            return null;
        }
        if (response.getStatus() >= 400)
            throw failure(response);
        return response.getEntity(Map.class);
    }

    /** A request answered with a resource; failures raise. */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> strict(Invocation<Map> invocation) {
        HttpResponse response = invocation.executeWithResponse();
        if (response.getStatus() >= 400)
            throw failure(response);
        if (response.getStatus() == 204) {
            HttpEntityHandler.closeQuietly(response);
            return new HashMap<>();
        }
        Map<String, Object> body = response.getEntity(Map.class);
        return body == null ? new HashMap<>() : body;
    }

    /** An asynchronous operation (202): the id at the end of its {@code Location} (the operation occurrence), or {@code null}. */
    protected String accepted(Invocation<Void> invocation) {
        HttpResponse response = invocation.executeWithResponse();
        if (response.getStatus() >= 400)
            throw failure(response);
        String location = response.header("Location");
        HttpEntityHandler.closeQuietly(response);
        return location == null ? null : location.replaceAll("[?#].*$", "").replaceAll("/+$", "").replaceAll("^.*/", "");
    }

    protected String accepted(String path, Map<String, ?> body) {
        return accepted(post(Void.class, path).entity(JsonBody.of(body == null ? Map.of() : body)));
    }

    /** A request without a result body. */
    protected ActionResponse act(Invocation<ActionResponse> invocation) {
        HttpResponse response = invocation.executeWithResponse();
        if (response.getStatus() >= 400) {
            ResponseException e = failure(response);
            return ActionResponse.actionFailed(e.getMessage(), response.getStatus());
        }
        HttpEntityHandler.closeQuietly(response);
        return ActionResponse.actionSuccess(response.getStatus());
    }
}
