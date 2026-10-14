package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;

/** Filters of {@code GET /credentials}. */
public class CredentialListOptions {

    private final Map<String, Object> options = new LinkedHashMap<>();

    public static CredentialListOptions create() {
        return new CredentialListOptions();
    }

    public CredentialListOptions userId(String userId) { options.put("user_id", userId); return this; }
    public CredentialListOptions type(String type) { options.put("type", type); return this; }

    public Map<String, Object> getOptions() {
        return options;
    }
}
