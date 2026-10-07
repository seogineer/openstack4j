package org.openstack4j.model.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A snapshot of all shares of a share group. Fields without a getter are in getAttributes(). */
public interface ShareGroupSnapshot extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getStatus();
    String getShareGroupId();
    String getProjectId();
    List<Map<String, Object>> getMembers();
    String getCreatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
