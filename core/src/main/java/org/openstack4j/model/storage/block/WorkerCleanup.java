package org.openstack4j.model.storage.block;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** Result of {@code POST /workers/cleanup} (3.24+). */
public interface WorkerCleanup extends ModelEntity {
    List<? extends CleanedService> getCleaning();
    List<? extends CleanedService> getUnavailable();

    interface CleanedService extends ModelEntity {
        String getId();
        String getHost();
        String getBinary();
        String getClusterName();
    }
}
