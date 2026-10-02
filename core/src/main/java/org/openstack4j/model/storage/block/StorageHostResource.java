package org.openstack4j.model.storage.block;


import org.openstack4j.model.ModelEntity;

/** Per-project resource usage of a host ({@code GET /os-hosts/{host}}); Cinder returns the numbers as strings. */
public interface StorageHostResource extends ModelEntity {
    String getProject();
    String getHost();
    String getVolumeCount();
    String getTotalVolumeGb();
    String getSnapshotCount();
    String getTotalSnapshotGb();
}
