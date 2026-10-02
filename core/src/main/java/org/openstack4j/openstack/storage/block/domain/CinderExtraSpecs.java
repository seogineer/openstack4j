package org.openstack4j.openstack.storage.block.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;

/** {@code {"extra_specs": {...}}} of a volume type. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderExtraSpecs implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("extra_specs")
    private Map<String, String> extraSpecs;

    public Map<String, String> getExtraSpecs() {
        return extraSpecs;
    }
}
