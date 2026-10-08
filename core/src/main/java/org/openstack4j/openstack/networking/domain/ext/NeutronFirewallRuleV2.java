package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.FirewallRuleV2;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("firewall_rule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronFirewallRuleV2 implements FirewallRuleV2 {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("action") private String action;
    @JsonProperty("protocol") private String protocol;
    @JsonProperty("ip_version") private Integer ipVersion;
    @JsonProperty("source_ip_address") private String sourceIpAddress;
    @JsonProperty("destination_ip_address") private String destinationIpAddress;
    @JsonProperty("source_port") private String sourcePort;
    @JsonProperty("destination_port") private String destinationPort;
    @JsonProperty("source_firewall_group_id") private String sourceFirewallGroupId;
    @JsonProperty("destination_firewall_group_id") private String destinationFirewallGroupId;
    @JsonProperty("enabled") private Boolean enabled;
    @JsonProperty("shared") private Boolean shared;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getAction() { return action; }
    @Override public String getProtocol() { return protocol; }
    @Override public Integer getIpVersion() { return ipVersion; }
    @Override public String getSourceIpAddress() { return sourceIpAddress; }
    @Override public String getDestinationIpAddress() { return destinationIpAddress; }
    @Override public String getSourcePort() { return sourcePort; }
    @Override public String getDestinationPort() { return destinationPort; }
    @Override public String getSourceFirewallGroupId() { return sourceFirewallGroupId; }
    @Override public String getDestinationFirewallGroupId() { return destinationFirewallGroupId; }
    @Override public Boolean isEnabled() { return enabled; }
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

    public static class NeutronFirewallRuleV2List extends ListResult<NeutronFirewallRuleV2> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("firewall_rules")
        private List<NeutronFirewallRuleV2> list;

        @Override
        protected List<NeutronFirewallRuleV2> value() {
            return list;
        }
    }
}
