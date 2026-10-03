package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A subnet floating IPs can be allocated from. */
public interface FloatingIPPool extends ModelEntity {
    String getSubnetId();
    String getSubnetName();
    String getNetworkId();
    String getProjectId();
}
