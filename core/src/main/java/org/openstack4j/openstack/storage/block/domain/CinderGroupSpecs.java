package org.openstack4j.openstack.storage.block.domain;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;

/** {@code {"group_specs": {...}}} of a group type. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderGroupSpecs implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("group_specs")
    private Map<String, String> groupSpecs;

    public Map<String, String> getGroupSpecs() {
        return groupSpecs;
    }
}
