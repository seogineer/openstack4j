package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareGroupType;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share_group_type")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareGroupType implements ShareGroupType {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("is_public") private Boolean isPublic;
    @JsonProperty("group_specs") private Map<String, Object> groupSpecs;
    @JsonProperty("share_types") private List<String> shareTypes;
    @JsonProperty("is_default") private Boolean isDefault;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public Boolean isPublic() { return isPublic; }
    @Override public Map<String, Object> getGroupSpecs() { return groupSpecs; }
    @Override public List<String> getShareTypes() { return shareTypes; }
    @Override public Boolean isDefault() { return isDefault; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class ManilaShareGroupTypeList extends ListResult<ManilaShareGroupType> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("share_group_types")
        private List<ManilaShareGroupType> list;

        @Override
        protected List<ManilaShareGroupType> value() {
            return list;
        }
    }
}
