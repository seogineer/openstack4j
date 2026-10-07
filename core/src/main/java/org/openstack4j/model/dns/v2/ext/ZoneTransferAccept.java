package org.openstack4j.model.dns.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** The acceptance of a zone transfer request. Fields without a getter are in getAttributes(). */
public interface ZoneTransferAccept extends ModelEntity {
    String getId();
    String getKey();
    String getZoneId();
    String getZoneTransferRequestId();
    String getProjectId();
    String getStatus();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
