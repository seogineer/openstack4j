package org.openstack4j.model.identity.v3.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Body of {@code POST /OS-TRUST/trusts}. */
public class TrustCreate {

    private final Map<String, Object> fields = new LinkedHashMap<>();
    private final List<Map<String, String>> roles = new ArrayList<>();

    private TrustCreate(String trustorUserId, String trusteeUserId, boolean impersonation) {
        fields.put("trustor_user_id", Objects.requireNonNull(trustorUserId));
        fields.put("trustee_user_id", Objects.requireNonNull(trusteeUserId));
        fields.put("impersonation", impersonation);
    }

    public static TrustCreate create(String trustorUserId, String trusteeUserId, boolean impersonation) {
        return new TrustCreate(trustorUserId, trusteeUserId, impersonation);
    }

    public TrustCreate projectId(String projectId) { fields.put("project_id", projectId); return this; }
    public TrustCreate expiresAt(Date expiresAt) { fields.put("expires_at", expiresAt.toInstant().toString()); return this; }
    public TrustCreate remainingUses(int remainingUses) { fields.put("remaining_uses", remainingUses); return this; }
    public TrustCreate allowRedelegation(boolean allow) { fields.put("allow_redelegation", allow); return this; }
    public TrustCreate redelegationCount(int count) { fields.put("redelegation_count", count); return this; }

    public TrustCreate roleIds(String... ids) {
        for (String id : ids) roles.add(Collections.singletonMap("id", id));
        return this;
    }

    public TrustCreate roleNames(String... names) {
        for (String name : names) roles.add(Collections.singletonMap("name", name));
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> body = new LinkedHashMap<>(fields);
        if (!roles.isEmpty()) body.put("roles", roles);
        return body;
    }
}
