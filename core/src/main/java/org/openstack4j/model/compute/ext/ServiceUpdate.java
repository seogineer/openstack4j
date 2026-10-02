package org.openstack4j.model.compute.ext;

import java.util.LinkedHashMap;
import java.util.Map;

/** Body of {@code PUT /os-services/{service_id}} (2.53+). Only the fields set are sent. */
public class ServiceUpdate {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public static ServiceUpdate create() {
        return new ServiceUpdate();
    }

    public ServiceUpdate enable() {
        fields.put("status", "enabled");
        fields.remove("disabled_reason");
        return this;
    }

    /** @param reason optional reason, {@code null} for none */
    public ServiceUpdate disable(String reason) {
        fields.put("status", "disabled");
        if (reason != null)
            fields.put("disabled_reason", reason);
        return this;
    }

    public ServiceUpdate forcedDown(boolean forcedDown) {
        fields.put("forced_down", forcedDown);
        return this;
    }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }
}
