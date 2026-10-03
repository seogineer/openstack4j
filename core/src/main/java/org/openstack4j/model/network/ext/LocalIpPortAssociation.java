package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** An association of a local IP with a port. */
public interface LocalIpPortAssociation extends ModelEntity {
    String getLocalIpId();
    String getLocalIpAddress();
    String getFixedPortId();
    String getFixedIp();
    String getHost();
}
