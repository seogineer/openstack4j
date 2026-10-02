package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** {@code {"traits": [...]}} */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementTraits implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("traits")
    private List<String> traits;

    public List<String> getTraits() {
        return traits == null ? Collections.emptyList() : traits;
    }
}
