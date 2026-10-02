package org.openstack4j.api.storage;

import java.util.List;
import org.openstack4j.model.storage.block.StorageCluster;
import org.openstack4j.model.storage.block.options.ClusterListOptions;

import org.openstack4j.common.RestService;

/** Cinder clusters ({@code /clusters}, block storage microversion 3.7+; admin). */
public interface BlockClusterService extends RestService {

    List<? extends StorageCluster> list();

    List<? extends StorageCluster> list(ClusterListOptions options);

    List<? extends StorageCluster> listDetail();

    List<? extends StorageCluster> listDetail(ClusterListOptions options);

    /** @param binary the service binary, or {@code null} for {@code cinder-volume} */
    StorageCluster get(String name, String binary);

    StorageCluster enable(String name, String binary);

    StorageCluster disable(String name, String binary, String reason);
}
