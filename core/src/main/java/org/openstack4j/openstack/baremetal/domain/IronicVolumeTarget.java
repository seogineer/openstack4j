package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.VolumeTarget;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicVolumeTarget implements VolumeTarget {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("node_uuid") private String nodeUuid;
    @JsonProperty("volume_type") private String volumeType;
    @JsonProperty("boot_index") private Integer bootIndex;
    @JsonProperty("volume_id") private String volumeId;
    @JsonProperty("properties") private Map<String, Object> properties;
    @JsonProperty("extra") private Map<String, Object> extra;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getUuid() { return uuid; }
    @Override public String getNodeUuid() { return nodeUuid; }
    @Override public String getVolumeType() { return volumeType; }
    @Override public Integer getBootIndex() { return bootIndex; }
    @Override public String getVolumeId() { return volumeId; }
    @Override public Map<String, Object> getProperties() { return properties; }
    @Override public Map<String, Object> getExtra() { return extra; }
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

    public static class IronicVolumeTargetList extends ListResult<IronicVolumeTarget> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("targets")
        private List<IronicVolumeTarget> list;

        @Override
        protected List<IronicVolumeTarget> value() {
            return list;
        }
    }
}
