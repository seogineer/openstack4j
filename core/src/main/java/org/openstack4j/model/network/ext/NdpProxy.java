package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A router NDP proxy publishing an IPv6 address of an internal port. */
public interface NdpProxy extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getRouterId();
    String getPortId();
    String getIpAddress();
    String getProjectId();
    Integer getRevisionNumber();
}
