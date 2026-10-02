package org.openstack4j.openstack.storage.block.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"meta": {"key": "value"}}} as returned by the single-item metadata APIs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderMetadataItem implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("meta")
    private Map<String, String> meta;

    public Map<String, String> getMeta() {
        return meta;
    }

    /** @return the single value, or {@code null} */
    public String value() {
        return meta == null || meta.isEmpty() ? null : meta.values().iterator().next();
    }
}
