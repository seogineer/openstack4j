package org.openstack4j.model.dns.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A zone shared with another project. Fields without a getter are in getAttributes(). */
public interface ZoneShare extends ModelEntity {
    String getId();
    String getZoneId();
    String getProjectId();
    String getTargetProjectId();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
