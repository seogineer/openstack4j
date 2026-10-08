package org.openstack4j.api.trove.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.trove.ext.Backup;
import org.openstack4j.model.trove.ext.options.BackupOptions;

/** Database backups ({@code /v1.0/{project_id}/backups}). */
public interface TroveBackupService extends RestService {

    /** @return the database backups */
    List<? extends Backup> list();

    /** @param filters query parameters such as {@code datastore}, {@code instance_id}, {@code all_projects} (admin), {@code project_id} (admin), {@code limit}, {@code marker} */
    List<? extends Backup> list(Map<String, String> filters);

    /** @return the backup, or {@code null} when it does not exist */
    Backup get(String id);

    Backup create(BackupOptions options);

    ActionResponse delete(String id);
}
