package org.openstack4j.model.storage.block;

import java.util.Date;

import org.openstack4j.model.ModelEntity;

/** A host running a block storage service ({@code GET /os-hosts}; admin). */
public interface StorageHost extends ModelEntity {
    String getHostName();
    String getService();
    String getZone();
    String getServiceStatus();
    String getServiceState();
    Date getLastUpdate();
}
