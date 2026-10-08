package org.openstack4j.model.trove.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A configuration group (database parameters applied to instances). Fields without a getter are in getAttributes(). */
public interface Configuration extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getDatastoreName();
    String getDatastoreVersionId();
    String getDatastoreVersionName();
    String getDatastoreVersionNumber();
    Integer getInstanceCount();
    Map<String, Object> getValues();
    String getCreated();
    String getUpdated();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
