package org.openstack4j.openstack.storage.block.internal;

import org.openstack4j.openstack.internal.MicroVersion;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.Builders;
import org.openstack4j.api.storage.BlockVolumeSnapshotService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.openstack.storage.block.domain.CinderVolumeSnapshot;
import org.openstack4j.openstack.storage.block.domain.CinderVolumeSnapshot.VolumeSnapshots;
import org.openstack4j.model.storage.block.options.SnapshotListOptions;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.storage.block.domain.CinderMetadata;
import org.openstack4j.openstack.storage.block.domain.CinderMetadataItem;

/**
 * OpenStack (Cinder) Volume Snapshot Operations API Implementation.
 *
 * @author Jeremy Unruh
 */
public class BlockVolumeSnapshotServiceImpl extends BaseBlockStorageServices implements BlockVolumeSnapshotService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends VolumeSnapshot> list() {
        return get(VolumeSnapshots.class, uri("/snapshots")).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends VolumeSnapshot> list(Map<String, String> filteringParams) {
        Invocation<VolumeSnapshots> volumeInvocation = buildInvocation(filteringParams);
        return volumeInvocation.execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VolumeSnapshot get(String snapshotId) {
        Objects.requireNonNull(snapshotId);
        return get(CinderVolumeSnapshot.class, uri("/snapshots/%s", snapshotId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse delete(String snapshotId) {
        Objects.requireNonNull(snapshotId);
        return deleteWithResponse(uri("/snapshots/%s", snapshotId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse update(String snapshotId, String name, String description) {
        Objects.requireNonNull(snapshotId);
        if (name == null && description == null)
            return ActionResponse.actionFailed("Both Name and Description are required", 412);

        return put(ActionResponse.class, uri("/snapshots/%s", snapshotId))
                .entity(Builders.volumeSnapshot().name(name).description(description).build())
                .execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VolumeSnapshot create(VolumeSnapshot snapshot) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(snapshot.getVolumeId());
        MicroVersion ceiling = snapshot instanceof CinderVolumeSnapshot && Boolean.FALSE.equals(((CinderVolumeSnapshot) snapshot).getForce()) ? V(65) : null;   // 3.66 rejects force=false
        return capped(post(CinderVolumeSnapshot.class, uri("/snapshots")), ceiling).entity(snapshot).execute();
    }

    private Invocation<VolumeSnapshots> buildInvocation(Map<String, String> filteringParams) {
        Invocation<VolumeSnapshots> volumeInvocation = get(VolumeSnapshots.class, "/snapshots");
        if (filteringParams == null) {
            return volumeInvocation;
        } else {
            for (Map.Entry<String, String> entry : filteringParams.entrySet()) {
                volumeInvocation = volumeInvocation.param(entry.getKey(), entry.getValue());
            }
        }
        return volumeInvocation;
    }

    @Override
    public List<? extends VolumeSnapshot> listDetail(SnapshotListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Snapshot list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(VolumeSnapshots.class, uri("/snapshots/detail")).params(options.toQueryParams()).execute().getList();
    }

    private ActionResponse action(String snapshotId, String action, Map<String, ?> body) {
        return post(ActionResponse.class, uri("/snapshots/%s/action", snapshotId)).entity(JsonBody.of(action, body)).execute();
    }

    @Override
    public Map<String, String> metadata(String snapshotId) {
        Objects.requireNonNull(snapshotId);
        CinderMetadata result = get(CinderMetadata.class, uri("/snapshots/%s/metadata", snapshotId)).execute();
        return result == null || result.getMetadata() == null ? Collections.emptyMap() : result.getMetadata();
    }

    @Override
    public Map<String, String> setMetadata(String snapshotId, Map<String, String> metadata) {
        Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(metadata);
        return post(CinderMetadata.class, uri("/snapshots/%s/metadata", snapshotId)).entity(JsonBody.of("metadata", metadata)).execute().getMetadata();
    }

    @Override
    public Map<String, String> replaceMetadata(String snapshotId, Map<String, String> metadata) {
        Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(metadata);
        return put(CinderMetadata.class, uri("/snapshots/%s/metadata", snapshotId)).entity(JsonBody.of("metadata", metadata)).execute().getMetadata();
    }

    @Override
    public String metadataItem(String snapshotId, String key) {
        Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(key);
        CinderMetadataItem item = get(CinderMetadataItem.class, uri("/snapshots/%s/metadata/%s", snapshotId, key)).execute();
        return item == null ? null : item.value();
    }

    @Override
    public String updateMetadataItem(String snapshotId, String key, String value) {
        Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(key);
        CinderMetadataItem item = put(CinderMetadataItem.class, uri("/snapshots/%s/metadata/%s", snapshotId, key))
                .entity(JsonBody.of("meta", Collections.singletonMap(key, value))).execute();
        return item == null ? null : item.value();
    }

    @Override
    public ActionResponse deleteMetadataItem(String snapshotId, String key) {
        Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(key);
        return deleteWithResponse(uri("/snapshots/%s/metadata/%s", snapshotId, key)).execute();
    }

    @Override
    public ActionResponse resetStatus(String snapshotId, String status) {
        return action(Objects.requireNonNull(snapshotId), "os-reset_status", Collections.singletonMap("status", Objects.requireNonNull(status)));
    }

    @Override
    public ActionResponse forceDelete(String snapshotId) {
        return action(Objects.requireNonNull(snapshotId), "os-force_delete", Collections.emptyMap());
    }

    @Override
    public ActionResponse unmanage(String snapshotId) {
        return action(Objects.requireNonNull(snapshotId), "os-unmanage", Collections.emptyMap());
    }

    @Override
    public ActionResponse updateStatus(String snapshotId, String status, String progress) {
        Objects.requireNonNull(snapshotId);
        Objects.requireNonNull(status);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        if (progress != null)
            body.put("progress", progress);
        return action(snapshotId, "os-update_snapshot_status", body);
    }
}
