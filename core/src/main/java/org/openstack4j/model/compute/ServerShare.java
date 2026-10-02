package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/** A Manila share attached to a server (2.97+). */
public interface ServerShare extends ModelEntity {
    String getShareId();
    String getStatus();
    String getTag();
    /** Admin only. */
    String getExportLocation();
    /** Admin only. */
    String getUuid();
}
