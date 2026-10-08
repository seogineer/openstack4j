package org.openstack4j.openstack.trove.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.trove.ext.TroveBackupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.trove.ext.Backup;
import org.openstack4j.model.trove.ext.options.BackupOptions;
import org.openstack4j.openstack.trove.domain.ext.TroveBackup;
import org.openstack4j.openstack.trove.domain.ext.TroveBackup.TroveBackupList;

public class TroveBackupServiceImpl extends BaseTroveExtService implements TroveBackupService {

    private static final String PATH = "/backups";
    private static final String ROOT = "backup";

    @Override
    public List<? extends Backup> list() {
        return list(null);
    }

    @Override
    public List<? extends Backup> list(Map<String, String> filters) {
        return listOf(TroveBackupList.class, PATH, filters);
    }

    @Override
    public Backup get(String id) {
        return show(TroveBackup.class, PATH + "/" + id(id));
    }

    @Override
    public Backup create(BackupOptions options) {
        return create(TroveBackup.class, PATH, ROOT, options);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(PATH + "/" + id(id));
    }
}
