package org.openstack4j.model.storage.block;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A group type (3.11+). */
public interface VolumeGroupType extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    Boolean isPublic();
    Map<String, String> getGroupSpecs();
}
