package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A QoS packet rate limit rule. */
public interface QosPacketRateLimitRule extends ModelEntity {
    String getId();
    Long getMaxKpps();
    Long getMaxBurstKpps();
    String getDirection();
    String getQosPolicyId();
}
