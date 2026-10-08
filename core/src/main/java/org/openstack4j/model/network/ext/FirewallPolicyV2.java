package org.openstack4j.model.network.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A FWaaS v2 firewall policy (an ordered list of rules). Fields without a getter are in getAttributes(). */
public interface FirewallPolicyV2 extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    List<String> getFirewallRules();
    Boolean isAudited();
    Boolean isShared();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
