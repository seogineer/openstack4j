package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A share server as the 2.49+ APIs return it (status kept as text). Fields without a getter are in getAttributes(). */
public interface ShareServerInfo extends ModelEntity {
    String getId();
    String getHost();
    String getStatus();
    String getShareNetworkId();
    String getShareNetworkName();
    String getIdentifier();
    Boolean isAutoDeletable();
    String getProjectId();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
