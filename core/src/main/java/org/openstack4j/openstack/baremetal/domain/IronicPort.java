package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.Port;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicPort implements Port {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("name") private String name;
    @JsonProperty("address") private String address;
    @JsonProperty("node_uuid") private String nodeUuid;
    @JsonProperty("portgroup_uuid") private String portgroupUuid;
    @JsonProperty("local_link_connection") private Map<String, Object> localLinkConnection;
    @JsonProperty("pxe_enabled") private Boolean pxeEnabled;
    @JsonProperty("physical_network") private String physicalNetwork;
    @JsonProperty("is_smartnic") private Boolean smartnic;
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
    @Override public String getPortgroupUuid() { return portgroupUuid; }
    @Override public Map<String, Object> getLocalLinkConnection() { return localLinkConnection; }
    @Override public Boolean isPxeEnabled() { return pxeEnabled; }
    @Override public String getPhysicalNetwork() { return physicalNetwork; }
    @Override public Boolean isSmartnic() { return smartnic; }
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

    public static class IronicPorts extends ListResult<IronicPort> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("ports")
        private List<IronicPort> list;

        @Override
        protected List<IronicPort> value() {
            return list;
        }
    }
}
