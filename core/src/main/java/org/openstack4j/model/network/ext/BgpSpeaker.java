package org.openstack4j.model.network.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A BGP speaker (neutron-dynamic-routing). Fields without a getter are in getAttributes(). */
public interface BgpSpeaker extends ModelEntity {
    String getId();
    String getName();
    Integer getLocalAs();
    Integer getIpVersion();
    List<String> getPeers();
    List<String> getNetworks();
    Boolean isAdvertiseFloatingIpHostRoutes();
    Boolean isAdvertiseTenantNetworks();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
