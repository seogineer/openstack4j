package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.*;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;
import org.openstack4j.api.storage.BlockGroupSnapshotService;
import org.openstack4j.model.storage.block.VolumeGroupSnapshot;
import org.openstack4j.model.storage.block.options.GroupSnapshotListOptions;
import org.openstack4j.openstack.storage.block.domain.CinderGroupSnapshot.GroupSnapshots;

public class BlockGroupSnapshotServiceImpl extends BaseBlockStorageServices implements BlockGroupSnapshotService {

    @Override public List<? extends VolumeGroupSnapshot> list() { return list(GroupSnapshotListOptions.create()); }
    @Override public List<? extends VolumeGroupSnapshot> listDetail() { return listDetail(GroupSnapshotListOptions.create()); }

    @Override
    public List<? extends VolumeGroupSnapshot> list(GroupSnapshotListOptions options) {
        requireOptions(options);
        return get(GroupSnapshots.class, uri("/group_snapshots")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public List<? extends VolumeGroupSnapshot> listDetail(GroupSnapshotListOptions options) {
        requireOptions(options);
        return get(GroupSnapshots.class, uri("/group_snapshots/detail")).params(options.toQueryParams()).execute().getList();
    }

    private void requireOptions(GroupSnapshotListOptions options) {
        Objects.requireNonNull(options);
        requireMicroVersion("Group snapshots", V(14));
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Group snapshot list options " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
    }

    @Override
    public VolumeGroupSnapshot get(String groupSnapshotId) {
        requireMicroVersion("Group snapshots", V(14));
        return get(CinderGroupSnapshot.class, uri("/group_snapshots/%s", Objects.requireNonNull(groupSnapshotId))).execute();
    }

    @Override
    public VolumeGroupSnapshot create(String groupId, String name, String description) {
        Objects.requireNonNull(groupId);
        requireMicroVersion("Group snapshots", V(14));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("group_id", groupId);
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        return post(CinderGroupSnapshot.class, uri("/group_snapshots")).entity(JsonBody.of("group_snapshot", body)).execute();
    }

    @Override
    public ActionResponse delete(String groupSnapshotId) {
        requireMicroVersion("Group snapshots", V(14));
        return deleteWithResponse(uri("/group_snapshots/%s", Objects.requireNonNull(groupSnapshotId))).execute();
    }

    @Override
    public ActionResponse resetStatus(String groupSnapshotId, String status) {
        requireMicroVersion("Group snapshot reset status", V(19));
        return post(ActionResponse.class, uri("/group_snapshots/%s/action", Objects.requireNonNull(groupSnapshotId)))
                .entity(JsonBody.of("reset_status", Collections.singletonMap("status", Objects.requireNonNull(status)))).execute();
    }
}
