package org.openstack4j.openstack.heat.domain.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/**
 * {@code {"resource_types": ["OS::Heat::RandomString", ...]}}; with {@code with_description=true} the elements are
 * {@code {"resource_type": ..., "description": ...}} objects, from which the names are taken.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeatResourceTypes implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("resource_types")
    private List<Object> types;

    public List<String> getTypes() {
        if (types == null)
            return Collections.emptyList();
        return types.stream().map(t -> t instanceof Map ? String.valueOf(((Map<?, ?>) t).get("resource_type")) : String.valueOf(t)).collect(Collectors.toList());
    }
}
