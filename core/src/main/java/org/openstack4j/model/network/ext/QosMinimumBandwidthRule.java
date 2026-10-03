package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A QoS minimum bandwidth rule. */
public interface QosMinimumBandwidthRule extends ModelEntity {
    String getId();
    Long getMinKbps();
    String getDirection();
    String getQosPolicyId();
}
