package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.FirewallPolicyV2;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("firewall_policy")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronFirewallPolicyV2 implements FirewallPolicyV2 {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("firewall_rules") private List<String> firewallRules;
    @JsonProperty("audited") private Boolean audited;
    @JsonProperty("shared") private Boolean shared;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public List<String> getFirewallRules() { return firewallRules; }
    @Override public Boolean isAudited() { return audited; }
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

    public static class NeutronFirewallPolicyV2List extends ListResult<NeutronFirewallPolicyV2> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("firewall_policies")
        private List<NeutronFirewallPolicyV2> list;

        @Override
        protected List<NeutronFirewallPolicyV2> value() {
            return list;
        }
    }
}
