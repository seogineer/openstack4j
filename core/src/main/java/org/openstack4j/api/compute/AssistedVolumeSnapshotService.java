package org.openstack4j.api.compute;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.AssistedVolumeSnapshot;

/** Assisted volume snapshots ({@code /os-assisted-volume-snapshots}), used by Cinder volume drivers; admin only. */
public interface AssistedVolumeSnapshotService extends RestService {

    /**
     * @param createInfo snapshot_id, type, new_file and optionally id
     */
    AssistedVolumeSnapshot create(String volumeId, Map<String, Object> createInfo);

    /**
     * @param deleteInfo volume_id and the other delete_info fields, sent as a JSON query parameter
     */
    ActionResponse delete(String snapshotId, Map<String, Object> deleteInfo);
}
