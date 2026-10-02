package org.openstack4j.openstack.storage.block.internal;

import org.openstack4j.openstack.internal.MicroVersion;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.Apis;
import org.openstack4j.api.Builders;
import org.openstack4j.api.storage.BlockVolumeService;
import org.openstack4j.api.storage.BlockVolumeTransferService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.VolumeTypeEncryption;
import org.openstack4j.model.storage.block.VolumeUploadImage;
import org.openstack4j.model.storage.block.options.UploadImageData;
import org.openstack4j.openstack.storage.block.domain.*;
import org.openstack4j.openstack.storage.block.domain.CinderVolume.Volumes;
import org.openstack4j.openstack.storage.block.domain.CinderVolumeType.VolumeTypes;
import java.util.LinkedHashMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.model.storage.block.VolumeSummary;
import org.openstack4j.model.storage.block.options.BlockStorageListOptions;
import org.openstack4j.model.storage.block.options.VolumeListOptions;
import org.openstack4j.model.storage.block.options.VolumeUpdateOptions;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import java.util.Collections;
import org.openstack4j.model.storage.block.options.VolumeMigrateRequest;

/**
 * Manages Volumes and Volume Type based operations against Block Storage (Cinder)
 *
 * @author Jeremy Unruh
 */
public class BlockVolumeServiceImpl extends BaseBlockStorageServices implements BlockVolumeService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends VolumeType> listVolumeTypes() {
        return get(VolumeTypes.class, uri("/types")).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Volume> list() {
        return get(Volumes.class, uri("/volumes/detail")).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Volume> list(Map<String, String> filteringParams) {
        Invocation<Volumes> volumeInvocation = buildInvocation(filteringParams);
        return volumeInvocation.execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Volume> listAll() {
        return get(Volumes.class, uri("/volumes/detail")).param("all_tenants", 1).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Volume get(String volumeId) {
        Objects.requireNonNull(volumeId);
        return get(CinderVolume.class, uri("/volumes/%s", volumeId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse delete(String volumeId) {
        Objects.requireNonNull(volumeId);
        return deleteWithResponse(uri("/volumes/%s", volumeId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse forceDelete(String volumeId) {
        Objects.requireNonNull(volumeId);
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId))
                .entity(new ForceDeleteAction())
                .execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse resetState(String volumeId, Volume.Status status) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(status);
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId))
                .entity(new ResetStatusAction(status))
                .execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse extend(String volumeId, Integer newSize) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(newSize);
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId))
                .entity(new ExtendAction(newSize))
                .execute();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse bootable(String volumeId, Boolean bootable) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(bootable);
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId))
                .entity(new SetBootableAction(bootable))
                .execute();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Volume create(Volume volume) {
        Objects.requireNonNull(volume);
        if (volume.getGroupId() != null)
            requireMicroVersion("Volume create option group_id", V(13));
        if (volume.getBackupId() != null)
            requireMicroVersion("Volume create option backup_id", V(47));
        MicroVersion ceiling = volume instanceof CinderVolume && ((CinderVolume) volume).hasBootable() ? V(52) : null;   // bootable is not in the 3.53 create schema
        Invocation<CinderVolume> req = capped(post(CinderVolume.class, uri("/volumes")), ceiling);
        if (volume.getSchedulerHints() != null && !volume.getSchedulerHints().isEmpty()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("volume", PLAIN_MAPPER.convertValue(volume, Map.class));
            body.put("OS-SCH-HNT:scheduler_hints", volume.getSchedulerHints());
            return req.entity(JsonBody.of(body)).execute();
        }
        return req.entity(volume).execute();
    }

    /** Serialises a volume as the inner object (no root), honouring the model's Jackson annotations. */
    private static final ObjectMapper PLAIN_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse update(String volumeId, String name, String description) {
        Objects.requireNonNull(volumeId);
        if (name == null && description == null)
            return ActionResponse.actionFailed("Name and Description are both required", 412);

        return put(ActionResponse.class, uri("/volumes/%s", volumeId))
                .entity(Builders.volume().name(name).description(description).build())
                .execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVolumeType(String volumeTypeId) {
        Objects.requireNonNull(volumeTypeId);
        delete(Void.class, uri("/types/%s", volumeTypeId)).execute();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VolumeType createVolumeType(VolumeType volumeType) {
        Objects.requireNonNull(volumeType);
        return post(CinderVolumeType.class, uri("/types")).entity(volumeType).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VolumeTypeEncryption createVolumeTypeEncryption(String volumeTypeId,
            VolumeTypeEncryption volumeTypeEncryption) {
        Objects.requireNonNull(volumeTypeEncryption);
        Objects.requireNonNull(volumeTypeId);
        return post(CinderVolumeTypeEncryption.class, uri("/types/%s/encryption", volumeTypeId))
                .entity(volumeTypeEncryption).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VolumeTypeEncryption getVolumeTypeEncryption(String volumeTypeId) {
        Objects.requireNonNull(volumeTypeId);
        return get(CinderVolumeTypeEncryptionFetch.class, uri("/types/%s/encryption", volumeTypeId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVolumeTypeEncryption(String volumeTypeId, String encryptionId) {
        Objects.requireNonNull(encryptionId);
        Objects.requireNonNull(volumeTypeId);
        delete(Void.class, uri("/types/%s/encryption/%s", volumeTypeId, encryptionId)).execute();
    }

    @Override
    public ActionResponse migrate(String volumeId, String hostService, boolean forceHostCopy) {
        CinderVolumeMigration migration = new CinderVolumeMigration(hostService, forceHostCopy);
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId))
                .entity(migration)
                .execute();
    }

    @Override
    public VolumeUploadImage uploadToImage(String volumeId, UploadImageData data) {
        Objects.requireNonNull(volumeId, "volumeId");
        Objects.requireNonNull(data, "UploadImageData");

        if (data.getVisibility() != null || data.getProtectedImage() != null)
            requireMicroVersion("Upload to image visibility/protected", V(1));
        return post(CinderVolumeUploadImage.class, uri("/volumes/%s/action", volumeId))
                .entity(CinderUploadImageData.create(data))
                .execute();
    }

    @Override
    public BlockVolumeTransferService transfer() {
        return Apis.get(BlockVolumeTransferService.class);
    }

    private Invocation<Volumes> buildInvocation(Map<String, String> filteringParams) {
        Invocation<Volumes> volumeInvocation = get(Volumes.class, "/volumes/detail");
        if (filteringParams == null) {
            return volumeInvocation;
        } else {
            for (Map.Entry<String, String> entry : filteringParams.entrySet()) {
                volumeInvocation = volumeInvocation.param(entry.getKey(), entry.getValue());
            }
        }
        return volumeInvocation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse readOnlyModeUpdate(String volumeId, boolean readonly) {
        Objects.requireNonNull(volumeId);
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId))
                .entity(new UpdateReadOnlyFlagAction(readonly))
                .execute();
    }

    /**
     * <p>Description:Attach volume to a server</p>
     * Volume status must be available.
     * You should set instanceId or hostName.
     * <p>Author:Wang Ting/王婷</p>
     *
     * @return ActionResponse
     */
    @Override
    public ActionResponse attach(String volumeId, String instanceId, String mountpoint, String hostName) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(instanceId);
        Objects.requireNonNull(mountpoint);
        Objects.requireNonNull(hostName);
        AttachAction attach = new AttachAction(instanceId, mountpoint, hostName);
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId)).entity(attach).execute();
    }

    /**
     * <p>Description:Force detach a volume</p>
     * <p>Author:Wang Ting/王婷</p>
     *
     * @Title: forceDetach
     * @see org.openstack4j.api.storage.BlockVolumeService#forceDetach(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ActionResponse forceDetach(String volumeId, String initiator, String attachmentId) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(initiator);
        Objects.requireNonNull(attachmentId);
        ForceDetachConnector connector = new ForceDetachConnector(initiator);
        ForceDetachAction detach = new ForceDetachAction(attachmentId, connector);
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId)).entity(detach).execute();
    }

    /**
     * Detach volume from server
     *
     * @author capitek-xuning（首信科技-徐宁）
     */
    @Override
    public ActionResponse detach(String volumeId, String attachmentId) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(attachmentId);
        DetachAction detach = new DetachAction(attachmentId);
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId)).entity(detach).execute();
    }

    @Override
    public List<? extends Volume> list(VolumeListOptions options) {
        Objects.requireNonNull(options);
        requireOptions("Volume list filters", options);
        return get(Volumes.class, uri("/volumes/detail")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public VolumeSummary summary() {
        requireMicroVersion("Volume summary", V(12));
        return get(CinderVolumeSummary.class, uri("/volumes/summary")).execute();
    }

    @Override
    public VolumeSummary summary(VolumeListOptions options) {
        Objects.requireNonNull(options);
        requireMicroVersion("Volume summary", V(12));
        requireOptions("Volume summary filters", options);
        return get(CinderVolumeSummary.class, uri("/volumes/summary")).params(options.toQueryParams()).execute();
    }

    @Override
    public Volume update(String volumeId, VolumeUpdateOptions options) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(options);
        return put(CinderVolume.class, uri("/volumes/%s", volumeId)).entity(JsonBody.of("volume", options.toMap())).execute();
    }

    private void requireOptions(String what, BlockStorageListOptions<?> options) {
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion(what + " " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
    }

    private ActionResponse action(String volumeId, String action, Map<String, ?> body) {
        return post(ActionResponse.class, uri("/volumes/%s/action", volumeId)).entity(JsonBody.of(action, body)).execute();
    }

    @Override
    public Map<String, String> metadata(String volumeId) {
        Objects.requireNonNull(volumeId);
        CinderMetadata result = get(CinderMetadata.class, uri("/volumes/%s/metadata", volumeId)).execute();
        return result == null ? Collections.emptyMap() : result.getMetadata();
    }

    @Override
    public Map<String, String> setMetadata(String volumeId, Map<String, String> metadata) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(metadata);
        return post(CinderMetadata.class, uri("/volumes/%s/metadata", volumeId)).entity(JsonBody.of("metadata", metadata)).execute().getMetadata();
    }

    @Override
    public Map<String, String> replaceMetadata(String volumeId, Map<String, String> metadata) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(metadata);
        return put(CinderMetadata.class, uri("/volumes/%s/metadata", volumeId)).entity(JsonBody.of("metadata", metadata)).execute().getMetadata();
    }

    @Override
    public String metadataItem(String volumeId, String key) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(key);
        CinderMetadataItem item = get(CinderMetadataItem.class, uri("/volumes/%s/metadata/%s", volumeId, key)).execute();
        return item == null ? null : item.value();
    }

    @Override
    public String updateMetadataItem(String volumeId, String key, String value) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(key);
        CinderMetadataItem item = put(CinderMetadataItem.class, uri("/volumes/%s/metadata/%s", volumeId, key))
                .entity(JsonBody.of("meta", Collections.singletonMap(key, value))).execute();
        return item == null ? null : item.value();
    }

    @Override
    public ActionResponse deleteMetadataItem(String volumeId, String key) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(key);
        return deleteWithResponse(uri("/volumes/%s/metadata/%s", volumeId, key)).execute();
    }

    @Override
    public Map<String, String> imageMetadata(String volumeId) {
        Objects.requireNonNull(volumeId);
        CinderMetadata result = post(CinderMetadata.class, uri("/volumes/%s/action", volumeId))
                .entity(JsonBody.of("os-show_image_metadata", Collections.emptyMap())).execute();
        return result == null ? Collections.emptyMap() : result.getMetadata();
    }

    @Override
    public Map<String, String> setImageMetadata(String volumeId, Map<String, String> metadata) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(metadata);
        return post(CinderMetadata.class, uri("/volumes/%s/action", volumeId))
                .entity(JsonBody.of("os-set_image_metadata", Collections.singletonMap("metadata", metadata))).execute().getMetadata();
    }

    @Override
    public ActionResponse unsetImageMetadata(String volumeId, String key) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(key);
        return action(volumeId, "os-unset_image_metadata", Collections.singletonMap("key", key));
    }

    @Override
    public ActionResponse revertToSnapshot(String volumeId, String snapshotId) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(snapshotId);
        requireMicroVersion("Revert to snapshot", V(40));
        return action(volumeId, "revert", Collections.singletonMap("snapshot_id", snapshotId));
    }

    @Override
    public ActionResponse reimage(String volumeId, String imageId, boolean reimageReserved) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(imageId);
        requireMicroVersion("Reimage", V(68));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("image_id", imageId);
        body.put("reimage_reserved", reimageReserved);
        return action(volumeId, "os-reimage", body);
    }

    @Override
    public ActionResponse completeExtend(String volumeId, boolean error) {
        Objects.requireNonNull(volumeId);
        requireMicroVersion("Extend completion", V(71));
        return action(volumeId, "os-extend_volume_completion", Collections.singletonMap("error", error));
    }

    @Override
    public ActionResponse retype(String volumeId, String newType, String migrationPolicy) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(newType);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("new_type", newType);
        if (migrationPolicy != null)
            body.put("migration_policy", migrationPolicy);
        return action(volumeId, "os-retype", body);
    }

    @Override
    public ActionResponse migrate(String volumeId, VolumeMigrateRequest request) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(request);
        if (request.getCluster() != null)
            requireMicroVersion("Migrate to a cluster", V(16));
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.getHost() != null) body.put("host", request.getHost());
        if (request.getCluster() != null) body.put("cluster", request.getCluster());
        if (request.getForceHostCopy() != null) body.put("force_host_copy", request.getForceHostCopy());
        if (request.getLockVolume() != null) body.put("lock_volume", request.getLockVolume());
        return action(volumeId, "os-migrate_volume", body);
    }

    @Override
    public ActionResponse completeMigration(String volumeId, String newVolumeId, boolean error) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(newVolumeId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("new_volume", newVolumeId);
        body.put("error", error);
        return action(volumeId, "os-migrate_volume_completion", body);
    }

    @Override public ActionResponse unmanage(String volumeId) { return action(Objects.requireNonNull(volumeId), "os-unmanage", Collections.emptyMap()); }
    @Override public ActionResponse reserve(String volumeId) { return action(Objects.requireNonNull(volumeId), "os-reserve", Collections.emptyMap()); }
    @Override public ActionResponse unreserve(String volumeId) { return action(Objects.requireNonNull(volumeId), "os-unreserve", Collections.emptyMap()); }
    @Override public ActionResponse beginDetaching(String volumeId) { return action(Objects.requireNonNull(volumeId), "os-begin_detaching", Collections.emptyMap()); }
    @Override public ActionResponse rollDetaching(String volumeId) { return action(Objects.requireNonNull(volumeId), "os-roll_detaching", Collections.emptyMap()); }

    @Override
    public Map<String, Object> initializeConnection(String volumeId, Map<String, Object> connector) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(connector);
        CinderConnectionInfo info = post(CinderConnectionInfo.class, uri("/volumes/%s/action", volumeId))
                .entity(JsonBody.of("os-initialize_connection", Collections.singletonMap("connector", connector))).execute();
        return info == null ? Collections.emptyMap() : info.getConnectionInfo();
    }

    @Override
    public ActionResponse terminateConnection(String volumeId, Map<String, Object> connector) {
        Objects.requireNonNull(volumeId);
        Objects.requireNonNull(connector);
        return action(volumeId, "os-terminate_connection", Collections.singletonMap("connector", connector));
    }

    @Override
    public ActionResponse setStatus(String volumeId, String status, String attachStatus, String migrationStatus) {
        Objects.requireNonNull(volumeId);
        Map<String, Object> body = new LinkedHashMap<>();
        if (status != null) body.put("status", status);
        if (attachStatus != null) body.put("attach_status", attachStatus);
        if (migrationStatus != null) body.put("migration_status", migrationStatus);
        return action(volumeId, "os-reset_status", body);
    }
}
