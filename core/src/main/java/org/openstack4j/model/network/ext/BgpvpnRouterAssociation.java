package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A router associated with a BGP VPN. Fields without a getter are in getAttributes(). */
public interface BgpvpnRouterAssociation extends ModelEntity {
    String getId();
    String getRouterId();
    Boolean isAdvertiseExtraRoutes();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
