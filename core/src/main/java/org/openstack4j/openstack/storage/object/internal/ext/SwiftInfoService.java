package org.openstack4j.openstack.storage.object.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.BaseOpenStackService;

/**
 * Swift's cluster-level calls that live outside the account URL: {@code GET /info} and the list_endpoints middleware
 * ({@code GET /endpoints/<account>/<container>/<object>}).
 */
public final class SwiftInfoService extends BaseOpenStackService {

    /** {@code http://host:8080/v1/AUTH_x} becomes {@code http://host:8080} (or the path before {@code /v1}). */
    static final Function<String, String> ROOT = url -> url.replaceAll("/+$", "").replaceAll("/v1(/.*)?$", "");

    /** {@code http://host:8080/v1/AUTH_x} becomes {@code http://host:8080/endpoints/AUTH_x}; a URL without {@code /v1} gets {@code /endpoints} appended. */
    static final Function<String, String> ENDPOINTS = url -> {
        String trimmed = url.replaceAll("/+$", "");
        return trimmed.matches(".*/v1(/.*)?$") ? trimmed.replaceFirst("/v1(/|$)", "/endpoints$1") : trimmed + "/endpoints";
    };

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
        StringBuilder path = new StringBuilder();
        if (container != null) {
            path.append('/').append(container);
            if (object != null)
                path.append('/').append(Objects.requireNonNull(object));
        }
        List<String> endpoints = new SwiftInfoService(ENDPOINTS).get(List.class, path.length() == 0 ? "" : path.toString()).execute();
        return endpoints == null ? Collections.emptyList() : endpoints;
    }
}
