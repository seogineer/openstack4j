package org.openstack4j.model.storage.block;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A replication target of a group (3.38+). */
public interface ReplicationTarget extends ModelEntity {
    String getBackendId();
    /** @return the other, driver specific keys */
    Map<String, String> getProperties();
}
