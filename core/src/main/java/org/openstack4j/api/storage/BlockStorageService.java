package org.openstack4j.api.storage;

import org.openstack4j.api.storage.ext.BlockStorageServiceService;
import org.openstack4j.common.RestService;
import org.openstack4j.model.storage.block.BlockLimits;

/**
 * Block Storage (Cinder) Service Operation API
 *
 * @author Jeremy Unruh
 */
public interface BlockStorageService extends RestService {

    /**
     * @return the Volume Service API
     */
    BlockVolumeService volumes();

    /**
     * @return the Volume Snapshot Service API
     */
    BlockVolumeSnapshotService snapshots();

    CinderZoneService zones();

    /**
     * Gets the Absolute limits used by this tenant
     *
     * @return the absolute limits
     */
    BlockLimits getLimits();

    /**
     * The block storage quota-set service.
     *
     * @return the quota-set service
     */
    BlockQuotaSetService quotaSets();

    /**
     * The block storage get_pools service.
     *
     * @return the scheduler stats service
     */
    SchedulerStatsGetPoolService schedulerStatsPools();

    /**
     * @return the Volume Service API
     */
    BlockVolumeBackupService backups();

    /**
     * The block storage service service
     *
     * @return ServiceService
     */
    BlockStorageServiceService services();

    /**
     * Opt-in block storage microversions (3.0 - 3.71); off by default.
     *
     * @return the block storage microversion service
     */
    BlockStorageMicroVersionService microVersions();

    /** @return volume attachments service (3.27+) */
    BlockAttachmentService attachments();

    /** @return user messages service (3.3+) */
    BlockMessageService messages();

    /** @return volume types service (CRUD, extra specs, access, encryption) */
    BlockVolumeTypeService volumeTypes();

    /** @return per-project default volume types (3.62+) */
    BlockDefaultTypeService defaultTypes();

    /** @return QoS specifications service */
    BlockQosSpecService qosSpecs();

    /** @return generic volume groups (3.13+) */
    BlockGroupService groups();

    /** @return group types (3.11+) */
    BlockGroupTypeService groupTypes();

    /** @return group snapshots (3.14+) */
    BlockGroupSnapshotService groupSnapshots();
}
