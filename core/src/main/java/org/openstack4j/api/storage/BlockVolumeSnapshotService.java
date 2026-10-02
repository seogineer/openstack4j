package org.openstack4j.api.storage;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.options.SnapshotListOptions;

/**
 * OpenStack (Cinder) Volume Snapshot Operations API.
 *
 * @author Jeremy Unruh
 */
public interface BlockVolumeSnapshotService extends RestService {

    /**
     * Lists detailed information for all Block Storage snapshots that the tenant who submits the request can access.
     *
     * @return List of VolumeSnapshot
     */
    List<? extends VolumeSnapshot> list();

    /**
     * Returns list of Block Storage snapshots filtered by parameters.
     *
     * @param filteringParams map (name, value) of filtering parameters
     */
    List<? extends VolumeSnapshot> list(Map<String, String> filteringParams);

    /**
     * Shows information for a specified snapshot.
     *
     * @param snapshotId the snapshot identifier
     * @return the volume snapshot or null
     */
    VolumeSnapshot get(String snapshotId);

    /**
     * Deletes a specified snapshot
     *
     * @param snapshotId the snapshot identifier
     * @return the action response
     */
    ActionResponse delete(String snapshotId);

    /**
     * Updates the Name and/or Description for the specified snapshot
     *
     * @param snapshotId the snapshot identifier
     * @param name the new name
     * @param description the new description
     * @return the action response
     */
    ActionResponse update(String snapshotId, String name, String description);

    /**
     * Creates a snapshot, which is a point-in-time copy of a volume. You can create a volume from the snapshot.
     * <p>
     * NOTE: the volume ID within the snapshot must be set or an NullPointerException will be thrown
     *
     * @param snapshot the snapshot to create
     * @return the newly created snapshot
     */
    VolumeSnapshot create(VolumeSnapshot snapshot);

    /**
     * Lists snapshots with details using typed filters ({@code GET /snapshots/detail}).
     */
    List<? extends VolumeSnapshot> listDetail(SnapshotListOptions options);

    /** All metadata of a snapshot ({@code GET /snapshots/{id}/metadata}). */
    Map<String, String> metadata(String snapshotId);

    /** Adds or updates metadata keys ({@code POST /snapshots/{id}/metadata}). */
    Map<String, String> setMetadata(String snapshotId, Map<String, String> metadata);

    /** Replaces all metadata ({@code PUT /snapshots/{id}/metadata}). */
    Map<String, String> replaceMetadata(String snapshotId, Map<String, String> metadata);

    /** One metadata value ({@code GET /snapshots/{id}/metadata/{key}}), or {@code null}. */
    String metadataItem(String snapshotId, String key);

    /** Sets one metadata value ({@code PUT /snapshots/{id}/metadata/{key}}). */
    String updateMetadataItem(String snapshotId, String key, String value);

    /** Deletes one metadata key ({@code DELETE /snapshots/{id}/metadata/{key}}). */
    ActionResponse deleteMetadataItem(String snapshotId, String key);

    /** Resets the status ({@code os-reset_status}; admin). */
    ActionResponse resetStatus(String snapshotId, String status);

    /** Force-deletes the snapshot ({@code os-force_delete}; admin). */
    ActionResponse forceDelete(String snapshotId);

    /**
     * Updates status and progress ({@code os-update_snapshot_status}).
     *
     * @param progress optional progress such as {@code 80%}, or {@code null}
     */
    ActionResponse updateStatus(String snapshotId, String status, String progress);

    /** Removes the snapshot from Cinder without deleting it on the backend ({@code os-unmanage}; admin). */
    ActionResponse unmanage(String snapshotId);
}
