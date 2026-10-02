package org.openstack4j.openstack.storage.block.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.*;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;
import org.openstack4j.api.storage.BlockManageableSnapshotService;
import org.openstack4j.model.storage.block.ManageableSnapshot;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.options.ManageableListOptions;
import org.openstack4j.model.storage.block.options.SnapshotManageRequest;
import org.openstack4j.openstack.storage.block.domain.CinderManageableSnapshot.ManageableSnapshots;

public class BlockManageableSnapshotServiceImpl extends BaseBlockStorageServices implements BlockManageableSnapshotService {

    @Override
    public List<? extends ManageableSnapshot> list(ManageableListOptions options) {
        requireOptions(options);
        return get(ManageableSnapshots.class, uri("/manageable_snapshots")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public List<? extends ManageableSnapshot> listDetail(ManageableListOptions options) {
        requireOptions(options);
        return get(ManageableSnapshots.class, uri("/manageable_snapshots/detail")).params(options.toQueryParams()).execute().getList();
    }

    private void requireOptions(ManageableListOptions options) {
        Objects.requireNonNull(options);
        requireMicroVersion("Manageable snapshots", V(8));
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Manageable list options " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
    }

    @Override
    public VolumeSnapshot manage(SnapshotManageRequest request) {
        Objects.requireNonNull(request);
        requireMicroVersion("Manageable snapshots", V(8));
        return post(CinderVolumeSnapshot.class, uri("/manageable_snapshots")).entity(JsonBody.of("snapshot", request.toMap())).execute();
    }
}
