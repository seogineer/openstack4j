package org.openstack4j.openstack.storage.object.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.OSClientSession;

/**
 * Swift's cluster-level calls that live outside the account URL: {@code GET /info} and the list_endpoints middleware
 * ({@code GET /endpoints/<account>/<container>/<object>}).
 */
public final class SwiftInfoService extends BaseOpenStackService {

    /** {@code http://host:8080/v1/AUTH_x} becomes {@code http://host:8080} (or the path before {@code /v1}). */
    static final Function<String, String> ROOT = url -> url.replaceAll("/+$", "").replaceAll("/v1(/.*)?$", "");

    /** {@code http://host:8080/v1/AUTH_x} becomes {@code http://host:8080/endpoints}; the account goes into the path. */
    static final Function<String, String> ENDPOINTS = url -> ROOT.apply(url) + "/endpoints";

    private SwiftInfoService(Function<String, String> endpoint) {
        super(ServiceType.OBJECT_STORAGE, endpoint);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> info() {
        Map<String, Object> info = new SwiftInfoService(ROOT).get(Map.class, "/info").execute();
        return info == null ? Collections.emptyMap() : info;
    }

    @SuppressWarnings("unchecked")
    public static List<String> endpoints(String container, String object) {
        String catalogUrl = OSClientSession.getCurrent().getEndpoint(ServiceType.OBJECT_STORAGE);
        List<String> endpoints = new SwiftInfoService(ENDPOINTS).get(List.class, endpointsPath(catalogUrl, container, object)).execute();
        return endpoints == null ? Collections.emptyList() : endpoints;
    }

    /**
     * @param catalogUrl the object storage URL of the catalog, e.g. {@code http://host:8080/v1/AUTH_x}
     * @return {@code /<account>[/<container>[/<object>]]} — never a trailing slash, which the middleware would read as an
     *         empty container
     */
    public static String endpointsPath(String catalogUrl, String container, String object) {
        StringBuilder path = new StringBuilder();
        Matcher account = Pattern.compile("/v1/([^/?#]+)").matcher(catalogUrl == null ? "" : catalogUrl);
        String last = null;
        while (account.find())
            last = account.group(1);
        if (last != null)
            path.append('/').append(last);
        if (container != null) {
            path.append('/').append(container);
            if (object != null)
                path.append('/').append(object);
        }
        return path.toString();
    }
}
