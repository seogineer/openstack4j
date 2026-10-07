package org.openstack4j.model.dns.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A zone import task (creates a zone from a zone file). Fields without a getter are in getAttributes(). */
public interface ZoneImport extends ModelEntity {
    String getId();
    String getZoneId();
    String getStatus();
    String getMessage();
    String getProjectId();
    Integer getVersion();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
