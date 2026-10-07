package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An access rule of a share (2.45 share-access-rules API). Fields without a getter are in getAttributes(). */
public interface ShareAccessRule extends ModelEntity {
    String getId();
    String getShareId();
    String getAccessType();
    String getAccessTo();
    String getAccessLevel();
    String getAccessKey();
    String getState();
    Map<String, Object> getMetadata();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
