package org.openstack4j.api.storage;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeGroupSnapshot;
import org.openstack4j.model.storage.block.options.GroupSnapshotListOptions;

/** Group snapshots ({@code /group_snapshots}, block storage microversion 3.14+; filters and paging 3.29+). */
public interface BlockGroupSnapshotService extends RestService {

    List<? extends VolumeGroupSnapshot> list();

    List<? extends VolumeGroupSnapshot> list(GroupSnapshotListOptions options);

    List<? extends VolumeGroupSnapshot> listDetail();

    List<? extends VolumeGroupSnapshot> listDetail(GroupSnapshotListOptions options);

    VolumeGroupSnapshot get(String groupSnapshotId);

    VolumeGroupSnapshot create(String groupId, String name, String description);

    ActionResponse delete(String groupSnapshotId);

    /** Resets the status ({@code reset_status}, 3.19+; admin). */
    ActionResponse resetStatus(String groupSnapshotId, String status);
}
