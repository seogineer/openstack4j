package org.openstack4j.model.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal inspection rule (conditions and actions run on inspection data). Fields without a getter are in getAttributes(). */
public interface InspectionRule extends ModelEntity {
    String getUuid();
    String getDescription();
    String getPhase();
    Integer getPriority();
    Boolean isSensitive();
    List<Map<String, Object>> getConditions();
    List<Map<String, Object>> getActions();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
