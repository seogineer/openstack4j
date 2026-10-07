package org.openstack4j.model.dns.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An offer to transfer a zone to another project. Fields without a getter are in getAttributes(). */
public interface ZoneTransferRequest extends ModelEntity {
    String getId();
    String getKey();
    String getZoneId();
    String getZoneName();
    String getProjectId();
    String getTargetProjectId();
    String getDescription();
    String getStatus();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
