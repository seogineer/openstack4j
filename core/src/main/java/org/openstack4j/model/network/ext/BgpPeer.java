package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A BGP peer of BGP speakers (neutron-dynamic-routing). Fields without a getter are in getAttributes(). */
public interface BgpPeer extends ModelEntity {
    String getId();
    String getName();
    String getPeerIp();
    Long getRemoteAs();
    String getAuthType();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
