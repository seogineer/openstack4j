package org.openstack4j.openstack.placement.v1.domain;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementResourceClass implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceClasses implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("resource_classes")
        private List<PlacementResourceClass> resourceClasses;

        public List<String> names() {
            return resourceClasses == null ? java.util.Collections.emptyList()
                    : resourceClasses.stream().map(PlacementResourceClass::getName).collect(Collectors.toList());
        }
    }
}
