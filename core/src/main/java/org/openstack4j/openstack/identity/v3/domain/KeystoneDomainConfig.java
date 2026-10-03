package org.openstack4j.openstack.identity.v3.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"config": {...}}}: a whole configuration (group → option → value) or one group (option → value). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneDomainConfig implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("config")
    private Map<String, Object> config;

    public Map<String, Object> getConfig() {
        return config;
    }
}
