package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"tags": [...]}} of a project. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneProjectTags implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("tags")
    private List<String> tags;

    public List<String> getTags() {
        return tags;
    }
}
