package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareSnapshotInfo;
import org.openstack4j.model.manila.ext.SnapshotAccess;
import org.openstack4j.model.manila.ext.SnapshotInstance;

/** The 2.12+ snapshot APIs: metadata, manage/unmanage, snapshot instances and access rules. */
public interface SnapshotExtService extends RestService {

    /**
     * {@code GET /snapshots}: id, name and links at the session microversion (floor 2.7).
     *
     * @param filters query parameters (share_id, status, name, name~ 2.36, project_id, all_tenants, limit, offset,
     *                sort_key, sort_dir, metadata 2.73 ...), may be null
     */
    List<? extends ShareSnapshotInfo> list(Map<String, String> filters);

    /**
     * {@code GET /snapshots/detail}: all fields. {@code project_id} and {@code user_id} need a session microversion of
     * 2.17 or higher, {@code metadata} 2.73.
     *
     * @param filters see {@link #list(Map)}, may be null
     */
    List<? extends ShareSnapshotInfo> listDetails(Map<String, String> filters);

    /** @return the snapshot, or {@code null} when it does not exist (floor 2.7; see {@link #listDetails(Map)} for fields) */
    ShareSnapshotInfo get(String snapshotId);

    /** @return the snapshot's metadata (2.73); a missing snapshot raises */
    Map<String, String> getMetadata(String snapshotId);

    /** @return one metadata value, or {@code null} (2.73) */
    String getMetadataItem(String snapshotId, String key);

    /** Adds or changes metadata items (2.73) and returns all of them. */
    Map<String, String> setMetadata(String snapshotId, Map<String, String> metadata);

    /** Replaces all metadata (2.73) and returns it. */
    Map<String, String> replaceMetadata(String snapshotId, Map<String, String> metadata);

    ActionResponse deleteMetadataItem(String snapshotId, String key);

    /**
     * Brings an existing back-end snapshot under Manila (2.12, admin).
     *
     * @param snapshot the {@code snapshot} fields: {@code share_id}, {@code provider_location} and optional {@code name},
     *                 {@code description}, {@code driver_options}
     */
    ShareSnapshotInfo manage(Map<String, ?> snapshot);

    /** Removes a snapshot from Manila without deleting it on the back end (2.12, admin). */
    ActionResponse unmanage(String snapshotId);

    /** @param filters query parameters such as {@code snapshot_id} @return the snapshot instances (2.19, admin) */
    List<? extends SnapshotInstance> listInstances(Map<String, String> filters);

    /** @return the snapshot instances with all fields (2.19, admin) */
    List<? extends SnapshotInstance> listInstancesDetail(Map<String, String> filters);

    /** @return the snapshot instance, or {@code null} when it does not exist (2.19) */
    SnapshotInstance getInstance(String snapshotInstanceId);

    /** @param status e.g. {@code available}, {@code error} (2.19, admin) */
    ActionResponse resetInstanceStatus(String snapshotInstanceId, String status);

    /**
     * Grants access to a mountable snapshot (2.32; IPv6 addresses from 2.38).
     *
     * @return the new rule, its state is usually {@code queued_to_apply}
     */
    SnapshotAccess grantAccess(String snapshotId, String accessType, String accessTo);

    /** Removes a snapshot access rule (2.32). */
    ActionResponse revokeAccess(String snapshotId, String accessId);

    /** @return the access rules of the snapshot (2.32); a missing snapshot raises */
    List<? extends SnapshotAccess> listAccess(String snapshotId);
}
