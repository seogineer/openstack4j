package org.openstack4j.model.manila.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An access rule of a share snapshot (2.32). Fields without a getter are in {@link #getAttributes()}. */
public interface SnapshotAccess extends ModelEntity {

    String getId();

    String getAccessType();

    String getAccessTo();

    /** @return queued_to_apply, applying, active, error, queued_to_deny, denying ... (text) */
    String getState();

    Map<String, Object> getAttributes();
}
