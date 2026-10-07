package org.openstack4j.model.dns.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A zone export task (creates a zone file of a zone). Fields without a getter are in getAttributes(). */
public interface ZoneExport extends ModelEntity {
    String getId();
    String getZoneId();
    String getStatus();
    String getMessage();
    String getLocation();
    String getProjectId();
    Integer getVersion();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
