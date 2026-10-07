package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.Portgroup;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicPortgroup implements Portgroup {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("name") private String name;
    @JsonProperty("address") private String address;
    @JsonProperty("node_uuid") private String nodeUuid;
    @JsonProperty("mode") private String mode;
    @JsonProperty("properties") private Map<String, Object> properties;
    @JsonProperty("standalone_ports_supported") private Boolean standalonePortsSupported;
    @JsonProperty("extra") private Map<String, Object> extra;
    @JsonProperty("internal_info") private Map<String, Object> internalInfo;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getUuid() { return uuid; }
    @Override public String getName() { return name; }
    @Override public String getAddress() { return address; }
    @Override public String getNodeUuid() { return nodeUuid; }
    @Override public String getMode() { return mode; }
    @Override public Map<String, Object> getProperties() { return properties; }
    @Override public Boolean isStandalonePortsSupported() { return standalonePortsSupported; }
    @Override public Map<String, Object> getExtra() { return extra; }
    @Override public Map<String, Object> getInternalInfo() { return internalInfo; }
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

    public static class IronicPortgroups extends ListResult<IronicPortgroup> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("portgroups")
        private List<IronicPortgroup> list;

        @Override
        protected List<IronicPortgroup> value() {
            return list;
        }
    }
}
