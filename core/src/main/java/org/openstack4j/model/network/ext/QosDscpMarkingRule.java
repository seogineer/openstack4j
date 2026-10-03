package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A QoS DSCP marking rule. */
public interface QosDscpMarkingRule extends ModelEntity {
    String getId();
    Integer getDscpMark();
    String getQosPolicyId();
}
