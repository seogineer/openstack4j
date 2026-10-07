package org.openstack4j.model.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A share group type (which share types a group may hold). Fields without a getter are in getAttributes(). */
public interface ShareGroupType extends ModelEntity {
    String getId();
    String getName();
    Boolean isPublic();
    Map<String, Object> getGroupSpecs();
    List<String> getShareTypes();
    Boolean isDefault();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
