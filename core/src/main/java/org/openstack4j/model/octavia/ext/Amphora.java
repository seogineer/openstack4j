package org.openstack4j.model.octavia.ext;

import org.openstack4j.model.ModelEntity;

/** An Octavia amphora: the service VM of an amphora-driver load balancer (admin). */
public interface Amphora extends ModelEntity {
    String getId();
    String getLoadBalancerId();
    String getComputeId();
    String getLbNetworkIp();
    String getVrrpIp();
    String getHaIp();
    String getVrrpPortId();
    String getHaPortId();
    String getCertExpiration();
    String getRole();
    String getStatus();
    String getVrrpInterface();
    Integer getVrrpId();
    Integer getVrrpPriority();
    String getCachedZone();
    String getImageId();
    String getComputeFlavor();
    String getCreatedAt();
    String getUpdatedAt();
}
