package org.openstack4j.api.storage;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.openstack.storage.block.domain.VolumeBackendPool;
import org.openstack4j.model.storage.block.options.PoolListOptions;

/**
 * Scheduler Stats Service for Cinder block storage.
 *
 * @author chenguofeng
 */
public interface SchedulerStatsGetPoolService extends RestService {
    /**
     * Lists all Volumes back-end storage pools.
     *
     * @return a list of all Volumes back-end storage pools
     */
    List<? extends VolumeBackendPool> pools();

    List<? extends VolumeBackendPool> poolsDetail();

    /** Pools with filters (capabilities 3.28+, volume type 3.35+). */
    List<? extends VolumeBackendPool> pools(PoolListOptions options);

    /** Detailed pools with filters (capabilities 3.28+, volume type 3.35+). */
    List<? extends VolumeBackendPool> poolsDetail(PoolListOptions options);
}
