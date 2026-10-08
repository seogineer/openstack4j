package org.openstack4j.model.network.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An IPsec site-to-site connection. Fields without a getter are in getAttributes(). */
public interface IpsecSiteConnection extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getStatus();
    String getVpnserviceId();
    String getIkepolicyId();
    String getIpsecpolicyId();
    String getLocalEpGroupId();
    String getPeerEpGroupId();
    String getPeerAddress();
    String getPeerId();
    List<String> getPeerCidrs();
    String getLocalId();
    String getPsk();
    String getInitiator();
    String getAuthMode();
    String getRouteMode();
    Integer getMtu();
    Map<String, Object> getDpd();
    Boolean isAdminStateUp();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
