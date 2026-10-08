package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A FWaaS v2 firewall rule. Fields without a getter are in getAttributes(). */
public interface FirewallRuleV2 extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getAction();
    String getProtocol();
    Integer getIpVersion();
    String getSourceIpAddress();
    String getDestinationIpAddress();
    String getSourcePort();
    String getDestinationPort();
    String getSourceFirewallGroupId();
    String getDestinationFirewallGroupId();
    Boolean isEnabled();
    Boolean isShared();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
