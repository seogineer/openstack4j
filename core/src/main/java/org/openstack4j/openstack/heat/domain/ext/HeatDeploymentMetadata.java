package org.openstack4j.openstack.heat.domain.ext;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"metadata": [...]}} of {@code GET /software_deployments/metadata/{server_id}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeatDeploymentMetadata implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("metadata")
    private List<Map<String, Object>> metadata;

    public List<Map<String, Object>> getMetadata() {
        return metadata;
    }
}
