package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A metering label rule selecting the traffic a label counts. */
public interface MeteringLabelRule extends ModelEntity {
    String getId();
    String getMeteringLabelId();
    String getDirection();
    String getRemoteIpPrefix();
    String getSourceIpPrefix();
    String getDestinationIpPrefix();
    Boolean isExcluded();
}
