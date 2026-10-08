package org.openstack4j.model.network.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A port associated with a BGP VPN. Fields without a getter are in getAttributes(). */
public interface BgpvpnPortAssociation extends ModelEntity {
    String getId();
    String getPortId();
    List<Map<String, Object>> getRoutes();
    Boolean isAdvertiseFixedIps();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
