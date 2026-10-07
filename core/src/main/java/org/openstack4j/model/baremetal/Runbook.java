package org.openstack4j.model.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal runbook (named clean or service steps). Fields without a getter are in getAttributes(). */
public interface Runbook extends ModelEntity {
    String getUuid();
    String getName();
    List<Map<String, Object>> getSteps();
    Boolean isPublic();
    String getOwner();
    Map<String, Object> getExtra();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
