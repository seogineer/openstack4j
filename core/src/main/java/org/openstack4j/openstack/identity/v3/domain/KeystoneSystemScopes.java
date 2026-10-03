package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"system": [...]}} of {@code GET /auth/system}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneSystemScopes implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("system")
    private List<Map<String, Object>> system;

    public List<Map<String, Object>> getSystem() {
        return system;
    }
}
