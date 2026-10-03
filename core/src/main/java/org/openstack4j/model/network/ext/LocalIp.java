package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A local IP: a virtual IP shared by many ports and reachable without routing. */
public interface LocalIp extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getProjectId();
    String getLocalPortId();
    String getNetworkId();
    String getLocalIpAddress();
    String getIpMode();
    Integer getRevisionNumber();
}
