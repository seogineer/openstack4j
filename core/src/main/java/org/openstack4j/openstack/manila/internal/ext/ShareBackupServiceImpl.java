package org.openstack4j.openstack.manila.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.ShareBackupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareBackup;
import org.openstack4j.model.manila.ext.options.ShareBackupCreate;
import org.openstack4j.model.manila.ext.options.ShareBackupUpdate;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareBackup;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareBackup.ManilaShareBackupList;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;

public class ShareBackupServiceImpl extends BaseManilaExtService implements ShareBackupService {

    private static final MicroVersion FLOOR = ManilaMicroVersions.V(80);

    @Override
    public List<? extends ShareBackup> list() {
        return list(null);
    }

    @Override
    public List<? extends ShareBackup> list(Map<String, String> filters) {
        return listOf(FLOOR, ManilaShareBackupList.class, "/share-backups/detail", filters);
    }

    @Override
    public ShareBackup get(String id) {
        return show(FLOOR, ManilaShareBackup.class, "/share-backups/" + id(id));
    }

    @Override
    public ShareBackup create(ShareBackupCreate create) {
        return create(FLOOR, ManilaShareBackup.class, "/share-backups", "share_backup", create);
    }

    @Override
    public ShareBackup update(String id, ShareBackupUpdate update) {
        return update(FLOOR, ManilaShareBackup.class, "/share-backups/" + id(id), "share_backup", update);
    }

    /** Share backups are experimental at every microversion. */
    @Override
    protected <R> Invocation<R> at(MicroVersion floor, Invocation<R> invocation, String feature) {
        return experimentalHeader(floor, ManilaMicroVersions.V(1000), super.at(floor, invocation, feature), feature);
    }

    @Override
    public ActionResponse restore(String id) {
        return action(FLOOR, "/share-backups/" + id(id), "restore", null);
    }

    @Override
    public ActionResponse restore(String id, String targetShareId) {
        String path = "/share-backups/" + id(id) + "/action";
        Map<String, Object> wrapper = new java.util.HashMap<>();
        wrapper.put("restore", java.util.Objects.requireNonNull(targetShareId, "targetShareId"));
        return at(ManilaMicroVersions.V(91), postWithResponse(path), path + " restore").entity(org.openstack4j.openstack.internal.microversion.JsonBody.of(wrapper)).execute();
    }

    @Override
    public ActionResponse resetStatus(String id, String status) {
        return action(FLOOR, "/share-backups/" + id(id), "reset_status", Map.of("status", java.util.Objects.requireNonNull(status, "status")));
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(FLOOR, "/share-backups/" + id(id));
    }
}
