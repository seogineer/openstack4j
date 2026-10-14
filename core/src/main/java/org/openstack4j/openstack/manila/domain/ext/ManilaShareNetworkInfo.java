package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareNetworkInfo;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share_network")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareNetworkInfo implements ShareNetworkInfo {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;
    @JsonProperty("status") private String status;
    @JsonProperty("security_service_update_support") private Boolean securityServiceUpdateSupport;
    @JsonProperty("network_allocation_update_support") private Boolean networkAllocationUpdateSupport;
    @JsonProperty("share_network_subnets") private List<ManilaShareNetworkSubnet> shareNetworkSubnets;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }
    @Override public String getStatus() { return status; }
    @Override public Boolean getSecurityServiceUpdateSupport() { return securityServiceUpdateSupport; }
    @Override public Boolean getNetworkAllocationUpdateSupport() { return networkAllocationUpdateSupport; }
    @Override public List<ManilaShareNetworkSubnet> getShareNetworkSubnets() { return shareNetworkSubnets; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class ManilaShareNetworkInfoList extends ListResult<ManilaShareNetworkInfo> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("share_networks")
        private List<ManilaShareNetworkInfo> list;

        @Override
        protected List<ManilaShareNetworkInfo> value() {
            return list;
        }
    }
}
