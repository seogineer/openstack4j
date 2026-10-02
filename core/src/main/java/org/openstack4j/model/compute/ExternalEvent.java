package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

public interface ExternalEvent extends ModelEntity {
    String getName();
    String getServerUuid();
    String getStatus();
    String getTag();
    /** Per-event result code: 200, 400, 404 or 422. */
    Integer getCode();
}
