package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A QoS type (named QoS specs for shares). Fields without a getter are in getAttributes(). */
public interface QosType extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    Map<String, Object> getSpecs();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
