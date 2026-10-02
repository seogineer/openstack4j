package org.openstack4j.api.storage;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.ReplicationTarget;
import org.openstack4j.model.storage.block.VolumeGroup;
import org.openstack4j.model.storage.block.options.GroupCreate;
import org.openstack4j.model.storage.block.options.GroupListOptions;

/** Generic volume groups ({@code /groups}, block storage microversion 3.13+). */
public interface BlockGroupService extends RestService {

    List<? extends VolumeGroup> list();

    List<? extends VolumeGroup> list(GroupListOptions options);

    List<? extends VolumeGroup> listDetail();

    List<? extends VolumeGroup> listDetail(GroupListOptions options);

    VolumeGroup get(String groupId);

    VolumeGroup create(GroupCreate request);

    /** Creates a group from a group snapshot or another group ({@code create-from-src}, 3.14+); give one of the two ids. */
    VolumeGroup createFromSource(String name, String description, String groupSnapshotId, String sourceGroupId);

    /** Updates name/description and adds or removes volumes; {@code null} leaves a part unchanged. */
    VolumeGroup update(String groupId, String name, String description, List<String> addVolumes, List<String> removeVolumes);

    /** Deletes the group ({@code delete} action), optionally with its volumes. */
    ActionResponse delete(String groupId, boolean deleteVolumes);

    /** Resets the status ({@code reset_status}, 3.20+; admin). */
    ActionResponse resetStatus(String groupId, String status);

    /** 3.38+ */
    ActionResponse enableReplication(String groupId);

    /** 3.38+ */
    ActionResponse disableReplication(String groupId);

    /** 3.38+ */
    ActionResponse failoverReplication(String groupId, boolean allowAttachedVolume, String secondaryBackendId);

    /** 3.38+ */
    List<? extends ReplicationTarget> listReplicationTargets(String groupId);
}
