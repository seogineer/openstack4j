package org.openstack4j.model.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A Designate pool (the name servers a zone is served from). Fields without a getter are in getAttributes(). */
public interface Pool extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    Map<String, Object> getPoolAttributes();
    List<Map<String, Object>> getNsRecords();
    String getProjectId();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
