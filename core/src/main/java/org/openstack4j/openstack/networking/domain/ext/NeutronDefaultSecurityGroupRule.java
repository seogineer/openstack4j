package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.DefaultSecurityGroupRule;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("default_security_group_rule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronDefaultSecurityGroupRule implements DefaultSecurityGroupRule {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("direction") private String direction;
    @JsonProperty("ethertype") private String ethertype;
    @JsonProperty("protocol") private String protocol;
    @JsonProperty("port_range_min") private Integer portRangeMin;
    @JsonProperty("port_range_max") private Integer portRangeMax;
    @JsonProperty("remote_ip_prefix") private String remoteIpPrefix;
    @JsonProperty("remote_group_id") private String remoteGroupId;
    @JsonProperty("remote_address_group_id") private String remoteAddressGroupId;
    @JsonProperty("description") private String description;
    @JsonProperty("used_in_default_sg") private Boolean usedInDefaultSg;
    @JsonProperty("used_in_non_default_sg") private Boolean usedInNonDefaultSg;

    @Override public String getId() { return id; }
    @Override public String getDirection() { return direction; }
    @Override public String getEthertype() { return ethertype; }
    @Override public String getProtocol() { return protocol; }
    @Override public Integer getPortRangeMin() { return portRangeMin; }
    @Override public Integer getPortRangeMax() { return portRangeMax; }
    @Override public String getRemoteIpPrefix() { return remoteIpPrefix; }
    @Override public String getRemoteGroupId() { return remoteGroupId; }
    @Override public String getRemoteAddressGroupId() { return remoteAddressGroupId; }
    @Override public String getDescription() { return description; }
    @Override public Boolean getUsedInDefaultSg() { return usedInDefaultSg; }
    @Override public Boolean getUsedInNonDefaultSg() { return usedInNonDefaultSg; }

    public static class Rules extends ListResult<NeutronDefaultSecurityGroupRule> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("default_security_group_rules")
        private List<NeutronDefaultSecurityGroupRule> list;

        @Override
        protected List<NeutronDefaultSecurityGroupRule> value() {
            return list;
        }
    }
}
