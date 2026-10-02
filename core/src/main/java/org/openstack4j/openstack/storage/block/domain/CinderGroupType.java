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
import org.openstack4j.model.storage.block.VolumeGroupType;

@JsonRootName("group_type")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderGroupType implements VolumeGroupType {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    @JsonProperty("is_public") private Boolean isPublic;
    @JsonProperty("group_specs") private Map<String, String> groupSpecs;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @JsonIgnore @Override public Boolean isPublic() { return isPublic; }
    @Override public Map<String, String> getGroupSpecs() { return groupSpecs; }

    public static class GroupTypes extends ListResult<CinderGroupType> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("group_types")
        private List<CinderGroupType> items;

        @Override
        protected List<CinderGroupType> value() {
            return items;
        }
    }
}
