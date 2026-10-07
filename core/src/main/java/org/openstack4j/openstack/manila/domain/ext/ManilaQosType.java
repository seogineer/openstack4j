package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.QosType;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("qos_type")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaQosType implements QosType {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("specs") private Map<String, Object> specs;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public Map<String, Object> getSpecs() { return specs; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class ManilaQosTypeList extends ListResult<ManilaQosType> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("qos_types")
        private List<ManilaQosType> list;

        @Override
        protected List<ManilaQosType> value() {
            return list;
        }
    }
}
