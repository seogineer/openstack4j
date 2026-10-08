package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A VPN service on a router (VPNaaS). Fields without a getter are in getAttributes(). */
public interface VpnService extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getRouterId();
    String getSubnetId();
    String getFlavorId();
    String getStatus();
    Boolean isAdminStateUp();
    String getExternalV4Ip();
    String getExternalV6Ip();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
