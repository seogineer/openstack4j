package org.openstack4j.api.storage;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.storage.block.ManageableSnapshot;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.options.ManageableListOptions;
import org.openstack4j.model.storage.block.options.SnapshotManageRequest;

/** Manageable snapshots ({@code /manageable_snapshots}, block storage microversion 3.8+; admin). */
public interface BlockManageableSnapshotService extends RestService {

    List<? extends ManageableSnapshot> list(ManageableListOptions options);

    List<? extends ManageableSnapshot> listDetail(ManageableListOptions options);

    VolumeSnapshot manage(SnapshotManageRequest request);
}
