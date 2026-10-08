package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.FirewallGroup;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("firewall_group")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronFirewallGroup implements FirewallGroup {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("ingress_firewall_policy_id") private String ingressFirewallPolicyId;
    @JsonProperty("egress_firewall_policy_id") private String egressFirewallPolicyId;
    @JsonProperty("ports") private List<String> ports;
    @JsonProperty("status") private String status;
    @JsonProperty("admin_state_up") private Boolean adminStateUp;
    @JsonProperty("shared") private Boolean shared;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getIngressFirewallPolicyId() { return ingressFirewallPolicyId; }
    @Override public String getEgressFirewallPolicyId() { return egressFirewallPolicyId; }
    @Override public List<String> getPorts() { return ports; }
    @Override public String getStatus() { return status; }
    @Override public Boolean isAdminStateUp() { return adminStateUp; }
    @Override public Boolean isShared() { return shared; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronFirewallGroupList extends ListResult<NeutronFirewallGroup> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("firewall_groups")
        private List<NeutronFirewallGroup> list;

        @Override
        protected List<NeutronFirewallGroup> value() {
            return list;
        }
    }
}
