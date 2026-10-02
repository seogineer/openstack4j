package org.openstack4j.openstack.storage.block.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"metadata": {...}}} as returned by the volume, snapshot and image-metadata APIs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderMetadata implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
