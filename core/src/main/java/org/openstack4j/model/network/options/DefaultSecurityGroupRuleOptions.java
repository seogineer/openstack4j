package org.openstack4j.model.network.options;

import java.util.Objects;

/** Body of a default security group rule create. */
public class DefaultSecurityGroupRuleOptions extends NeutronAttributes<DefaultSecurityGroupRuleOptions> {

    /** @param direction ingress or egress */
    public static DefaultSecurityGroupRuleOptions create(String direction) {
        return new DefaultSecurityGroupRuleOptions().put("direction", Objects.requireNonNull(direction));
    }

    @Override
    protected DefaultSecurityGroupRuleOptions self() {
        return this;
    }

    public DefaultSecurityGroupRuleOptions ethertype(String ethertype) { return put("ethertype", ethertype); }
    public DefaultSecurityGroupRuleOptions protocol(String protocol) { return put("protocol", protocol); }
    public DefaultSecurityGroupRuleOptions portRangeMin(Integer port) { return put("port_range_min", port); }
    public DefaultSecurityGroupRuleOptions portRangeMax(Integer port) { return put("port_range_max", port); }
    public DefaultSecurityGroupRuleOptions remoteIpPrefix(String prefix) { return put("remote_ip_prefix", prefix); }
    /** "PARENT" refers to the security group the rule is copied into. */
    public DefaultSecurityGroupRuleOptions remoteGroupId(String groupId) { return put("remote_group_id", groupId); }
    public DefaultSecurityGroupRuleOptions remoteAddressGroupId(String addressGroupId) { return put("remote_address_group_id", addressGroupId); }
    public DefaultSecurityGroupRuleOptions description(String description) { return put("description", description); }
    public DefaultSecurityGroupRuleOptions usedInDefaultSg(Boolean used) { return put("used_in_default_sg", used); }
    public DefaultSecurityGroupRuleOptions usedInNonDefaultSg(Boolean used) { return put("used_in_non_default_sg", used); }
}
