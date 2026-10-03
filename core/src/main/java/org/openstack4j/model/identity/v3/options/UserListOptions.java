package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters for {@code GET /v3/users}. */
public class UserListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();

    public static UserListOptions create() {
        return new UserListOptions();
    }

    private UserListOptions put(String key, Object value) {
        params.put(key, String.valueOf(value));
        return this;
    }

    public UserListOptions name(String name) { return put("name", name); }
    public UserListOptions domainId(String domainId) { return put("domain_id", domainId); }
    public UserListOptions enabled(boolean enabled) { return put("enabled", enabled); }
    /** Users federated through this identity provider. */
    public UserListOptions idpId(String idpId) { return put("idp_id", idpId); }
    public UserListOptions protocolId(String protocolId) { return put("protocol_id", protocolId); }
    public UserListOptions uniqueId(String uniqueId) { return put("unique_id", uniqueId); }
    /** Time comparison filter: {@code op} is lt, lte, gt, gte, eq or neq. */
    public UserListOptions passwordExpiresAt(String op, String isoTime) { return put("password_expires_at", op + ":" + isoTime); }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }
}
