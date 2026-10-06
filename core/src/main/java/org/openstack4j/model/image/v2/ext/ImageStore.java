package org.openstack4j.model.image.v2.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A Glance image store (multi-store). */
public interface ImageStore extends ModelEntity {
    String getId();
    String getDescription();
    Boolean isDefault();
    Boolean isReadOnly();
    String getType();
    Integer getWeight();
    Map<String, Object> getProperties();
}
