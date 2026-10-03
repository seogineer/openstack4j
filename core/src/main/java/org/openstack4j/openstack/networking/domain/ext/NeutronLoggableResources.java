package org.openstack4j.openstack.networking.domain.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"loggable_resources": [{"type": "security_group"}, ...]}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronLoggableResources implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("loggable_resources")
    private List<Map<String, String>> resources;

    /** @return the resource types that can be logged */
    public List<String> getTypes() {
        return resources == null ? Collections.emptyList() : resources.stream().map(r -> r.get("type")).collect(Collectors.toList());
    }
}
