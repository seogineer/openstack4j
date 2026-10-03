package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A QoS minimum packet rate rule. */
public interface QosMinimumPacketRateRule extends ModelEntity {
    String getId();
    Long getMinKpps();
    String getDirection();
    String getQosPolicyId();
}
