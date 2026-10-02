package org.openstack4j.model.storage.block;

import java.util.Date;

import org.openstack4j.model.ModelEntity;

/** A cinder-volume cluster (3.7+; replication fields 3.26+). */
public interface StorageCluster extends ModelEntity {
    String getName();
    String getBinary();
    String getState();
    String getStatus();
    String getDisabledReason();
    Integer getNumHosts();
    Integer getNumDownHosts();
    Date getLastHeartbeat();
    Date getCreatedAt();
    Date getUpdatedAt();
    String getReplicationStatus();
    Boolean getFrozen();
    String getActiveBackendId();
}
