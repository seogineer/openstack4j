package org.openstack4j.model.identity.v3.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Body of {@code POST /users/{id}/application_credentials}. */
public class ApplicationCredentialCreate {

    private final Map<String, Object> fields = new LinkedHashMap<>();
    private final List<Map<String, String>> roles = new ArrayList<>();
    private final List<Map<String, String>> accessRules = new ArrayList<>();

    private ApplicationCredentialCreate(String name) {
        fields.put("name", name);
    }

    public static ApplicationCredentialCreate create(String name) {
        return new ApplicationCredentialCreate(name);
    }

    public ApplicationCredentialCreate description(String description) { fields.put("description", description); return this; }
    /** A secret of your own; Keystone generates one when omitted. */
    public ApplicationCredentialCreate secret(String secret) { fields.put("secret", secret); return this; }
    /** ISO 8601 expiry is generated from the date (UTC). */
    public ApplicationCredentialCreate expiresAt(Date expiresAt) { fields.put("expires_at", expiresAt.toInstant().toString()); return this; }
    /** Allows the credential to create other application credentials and trusts. */
    public ApplicationCredentialCreate unrestricted(boolean unrestricted) { fields.put("unrestricted", unrestricted); return this; }

    public ApplicationCredentialCreate roleIds(String... ids) {
        for (String id : ids) roles.add(Collections.singletonMap("id", id));
        return this;
    }

    public ApplicationCredentialCreate roleNames(String... names) {
        for (String name : names) roles.add(Collections.singletonMap("name", name));
        return this;
    }

    /** Restricts the credential to one API call, for example ("compute", "GET", "/v2.1/servers"). */
    public ApplicationCredentialCreate accessRule(String service, String method, String path) {
        Map<String, String> rule = new LinkedHashMap<>();
        rule.put("service", service);
        rule.put("method", method);
        rule.put("path", path);
        accessRules.add(rule);
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> body = new LinkedHashMap<>(fields);
        if (!roles.isEmpty()) body.put("roles", roles);
        if (!accessRules.isEmpty()) body.put("access_rules", accessRules);
        return body;
    }
}
