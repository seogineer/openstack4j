package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareBackup;
import org.openstack4j.model.manila.ext.options.ShareBackupCreate;
import org.openstack4j.model.manila.ext.options.ShareBackupUpdate;

/** Share backups ({@code /v2/share-backups}, microversion 2.80). */
public interface ShareBackupService extends RestService {

    /** @return the share backups */
    List<? extends ShareBackup> list();

    /** @param filters query parameters such as {@code share_id}, {@code name}, {@code status}, {@code host}, {@code topic}, {@code limit}, {@code offset} */
    List<? extends ShareBackup> list(Map<String, String> filters);

    /** @return the share backup, or {@code null} when it does not exist */
    ShareBackup get(String id);

    ShareBackup create(ShareBackupCreate create);

    ShareBackup update(String id, ShareBackupUpdate update);

    /** Restores the backup to its share. */
    ActionResponse restore(String id);

    /** Restores the backup to another share (2.91). */
    ActionResponse restore(String id, String targetShareId);

    /** @param status e.g. {@code available}, {@code error} (admin) */
    ActionResponse resetStatus(String id, String status);

    ActionResponse delete(String id);
}
