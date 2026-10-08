package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A network associated with a BGP VPN. Fields without a getter are in getAttributes(). */
public interface BgpvpnNetworkAssociation extends ModelEntity {
    String getId();
    String getNetworkId();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
