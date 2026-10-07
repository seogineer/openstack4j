package org.openstack4j.openstack.manila.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.ShareGroupSnapshotService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareGroupSnapshot;
import org.openstack4j.model.manila.ext.options.ShareGroupSnapshotCreate;
import org.openstack4j.model.manila.ext.options.ShareGroupSnapshotUpdate;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareGroupSnapshot;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareGroupSnapshot.ManilaShareGroupSnapshotList;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;

public class ShareGroupSnapshotServiceImpl extends BaseManilaExtService implements ShareGroupSnapshotService {

    private static final MicroVersion FLOOR = ManilaMicroVersions.V(55);

    @Override
    public List<? extends ShareGroupSnapshot> list() {
        return list(null);
    }

    @Override
    public List<? extends ShareGroupSnapshot> list(Map<String, String> filters) {
        return listOf(FLOOR, ManilaShareGroupSnapshotList.class, "/share-group-snapshots/detail", filters);
    }

    @Override
    public ShareGroupSnapshot get(String id) {
        return show(FLOOR, ManilaShareGroupSnapshot.class, "/share-group-snapshots/" + id(id));
    }

    @Override
    public ShareGroupSnapshot create(ShareGroupSnapshotCreate create) {
        return create(FLOOR, ManilaShareGroupSnapshot.class, "/share-group-snapshots", "share_group_snapshot", create);
    }

    @Override
    public ShareGroupSnapshot update(String id, ShareGroupSnapshotUpdate update) {
        return update(FLOOR, ManilaShareGroupSnapshot.class, "/share-group-snapshots/" + id(id), "share_group_snapshot", update);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> listMembers(String id) {
        Map<String, Object> body = showStrict(FLOOR, Map.class, "/share-group-snapshots/" + id(id) + "/members");
        Object members = body == null ? null : body.get("share_group_snapshot_members");
        return members instanceof List ? (List<Map<String, Object>>) members : java.util.Collections.emptyList();
    }

    @Override
    public ActionResponse resetStatus(String id, String status) {
        return action(FLOOR, "/share-group-snapshots/" + id(id), "reset_status", Map.of("status", java.util.Objects.requireNonNull(status, "status")));
    }

    @Override
    public ActionResponse forceDelete(String id) {
        return action(FLOOR, "/share-group-snapshots/" + id(id), "force_delete", null);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(FLOOR, "/share-group-snapshots/" + id(id));
    }
}
