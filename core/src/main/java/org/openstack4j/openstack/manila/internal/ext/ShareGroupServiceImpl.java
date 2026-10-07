package org.openstack4j.openstack.manila.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.ShareGroupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareGroup;
import org.openstack4j.model.manila.ext.options.ShareGroupCreate;
import org.openstack4j.model.manila.ext.options.ShareGroupUpdate;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareGroup;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareGroup.ManilaShareGroupList;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;

public class ShareGroupServiceImpl extends BaseManilaExtService implements ShareGroupService {

    private static final MicroVersion FLOOR = ManilaMicroVersions.V(55);

    @Override
    public List<? extends ShareGroup> list() {
        return list(null);
    }

    @Override
    public List<? extends ShareGroup> list(Map<String, String> filters) {
        return listOf(FLOOR, ManilaShareGroupList.class, "/share-groups/detail", filters);
    }

    @Override
    public ShareGroup get(String id) {
        return show(FLOOR, ManilaShareGroup.class, "/share-groups/" + id(id));
    }

    @Override
    public ShareGroup create(ShareGroupCreate create) {
        return create(FLOOR, ManilaShareGroup.class, "/share-groups", "share_group", create);
    }

    @Override
    public ShareGroup update(String id, ShareGroupUpdate update) {
        return update(FLOOR, ManilaShareGroup.class, "/share-groups/" + id(id), "share_group", update);
    }

    @Override
    public ActionResponse resetStatus(String id, String status) {
        return action(FLOOR, "/share-groups/" + id(id), "reset_status", Map.of("status", java.util.Objects.requireNonNull(status, "status")));
    }

    @Override
    public ActionResponse forceDelete(String id) {
        return action(FLOOR, "/share-groups/" + id(id), "force_delete", null);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(FLOOR, "/share-groups/" + id(id));
    }
}
