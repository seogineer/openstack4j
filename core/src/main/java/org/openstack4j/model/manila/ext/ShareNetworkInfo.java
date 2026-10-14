package org.openstack4j.model.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/**
 * A share network as the session-microversion APIs return it. Which fields are present depends on the microversion;
 * fields without a getter (for example neutron_net_id and cidr below 2.51) are in getAttributes().
 */
public interface ShareNetworkInfo extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getProjectId();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the status (2.63+) */
    String getStatus();
    /** @return whether the security services can be updated (2.63+) */
    Boolean getSecurityServiceUpdateSupport();
    /** @return whether the network allocation can be updated (2.70+) */
    Boolean getNetworkAllocationUpdateSupport();
    /** @return the subnets (2.51+; from 2.78 each has metadata) */
    List<? extends ShareNetworkSubnet> getShareNetworkSubnets();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
