package org.openstack4j.model.compute;

import java.util.Date;

import org.openstack4j.model.ModelEntity;

/** An in-progress live migration of a server ({@code /servers/{id}/migrations}, 2.23+). */
public interface ServerMigration extends ModelEntity {
    String getId();
    /** 2.59+ */
    String getUuid();
    String getServerUuid();
    String getStatus();
    String getSourceCompute();
    String getSourceNode();
    String getDestCompute();
    String getDestNode();
    String getDestHost();
    Long getMemoryTotalBytes();
    Long getMemoryProcessedBytes();
    Long getMemoryRemainingBytes();
    Long getDiskTotalBytes();
    Long getDiskProcessedBytes();
    Long getDiskRemainingBytes();
    Date getCreatedAt();
    Date getUpdatedAt();
    /** 2.80+ */
    String getUserId();
    /** 2.80+ */
    String getProjectId();
}
