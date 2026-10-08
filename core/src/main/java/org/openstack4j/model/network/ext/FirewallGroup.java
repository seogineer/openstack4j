package org.openstack4j.model.network.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A FWaaS v2 firewall group (ingress/egress policies applied to ports). Fields without a getter are in getAttributes(). */
public interface FirewallGroup extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getIngressFirewallPolicyId();
    String getEgressFirewallPolicyId();
    List<String> getPorts();
    String getStatus();
    Boolean isAdminStateUp();
    Boolean isShared();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
