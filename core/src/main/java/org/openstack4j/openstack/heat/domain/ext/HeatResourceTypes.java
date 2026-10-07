package org.openstack4j.openstack.heat.domain.ext;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"resource_types": ["OS::Heat::RandomString", ...]}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeatResourceTypes implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_types")
    private List<String> types;

    public List<String> getTypes() {
        return types == null ? Collections.emptyList() : types;
    }
}
