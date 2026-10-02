package org.openstack4j.openstack.storage.block.internal;

import org.openstack4j.api.Apis;
import org.openstack4j.api.storage.*;
import org.openstack4j.api.storage.ext.BlockStorageServiceService;
import org.openstack4j.model.storage.block.BlockLimits;
import org.openstack4j.openstack.storage.block.domain.CinderBlockLimits;

/**
 * Block Storage (Cinder) Service Operation implementation
 *
 * @author Jeremy Unruh
 */
public class BlockStorageServiceImpl extends BaseBlockStorageServices implements BlockStorageService {

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockVolumeService volumes() {
        return Apis.get(BlockVolumeService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockVolumeSnapshotService snapshots() {
        return Apis.get(BlockVolumeSnapshotService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockLimits getLimits() {
        return get(CinderBlockLimits.class, "/limits").execute();
    }

    @Override
    public BlockLimits getLimits(String projectId) {
        java.util.Objects.requireNonNull(projectId);
        requireMicroVersion("Limits by project", BlockStorageMicroVersions.V(39));
        return get(CinderBlockLimits.class, "/limits").param("project_id", projectId).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockQuotaSetService quotaSets() {
        return Apis.get(BlockQuotaSetService.class);
    }

    @Override
    public CinderZoneService zones() {
        return Apis.get(CinderZoneService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchedulerStatsGetPoolService schedulerStatsPools() {
        return Apis.get(SchedulerStatsGetPoolService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockVolumeBackupService backups() {
        return Apis.get(BlockVolumeBackupService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockStorageMicroVersionService microVersions() {
        return Apis.get(BlockStorageMicroVersionService.class);
    }

    @Override
    public BlockAttachmentService attachments() {
        return Apis.get(BlockAttachmentService.class);
    }

    @Override
    public BlockMessageService messages() {
        return Apis.get(BlockMessageService.class);
    }

    @Override
    public BlockVolumeTypeService volumeTypes() {
        return Apis.get(BlockVolumeTypeService.class);
    }

    @Override
    public BlockDefaultTypeService defaultTypes() {
        return Apis.get(BlockDefaultTypeService.class);
    }

    @Override
    public BlockQosSpecService qosSpecs() {
        return Apis.get(BlockQosSpecService.class);
    }

    @Override
    public BlockGroupService groups() {
        return Apis.get(BlockGroupService.class);
    }

    @Override
    public BlockGroupTypeService groupTypes() {
        return Apis.get(BlockGroupTypeService.class);
    }

    @Override
    public BlockGroupSnapshotService groupSnapshots() {
        return Apis.get(BlockGroupSnapshotService.class);
    }

    @Override
    public BlockClusterService clusters() {
        return Apis.get(BlockClusterService.class);
    }

    @Override
    public BlockWorkerService workers() {
        return Apis.get(BlockWorkerService.class);
    }

    @Override
    public BlockHostService hosts() {
        return Apis.get(BlockHostService.class);
    }

    @Override
    public BlockCapabilityService capabilities() {
        return Apis.get(BlockCapabilityService.class);
    }

    @Override
    public BlockResourceFilterService resourceFilters() {
        return Apis.get(BlockResourceFilterService.class);
    }

    @Override
    public BlockExtensionService extensions() {
        return Apis.get(BlockExtensionService.class);
    }

    @Override
    public BlockVolumeTransferV3Service volumeTransfers() {
        return Apis.get(BlockVolumeTransferV3Service.class);
    }

    @Override
    public BlockManageableVolumeService manageableVolumes() {
        return Apis.get(BlockManageableVolumeService.class);
    }

    @Override
    public BlockManageableSnapshotService manageableSnapshots() {
        return Apis.get(BlockManageableSnapshotService.class);
    }

    @Override
    public BlockStorageServiceService services() {
        return Apis.get(BlockStorageServiceService.class);
    }

}
