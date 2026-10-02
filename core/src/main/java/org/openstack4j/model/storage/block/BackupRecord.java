package org.openstack4j.model.storage.block;

import org.openstack4j.model.ModelEntity;

/** An exported backup record ({@code GET /backups/{id}/export_record}), importable on another deployment. */
public interface BackupRecord extends ModelEntity {
    String getBackupService();
    String getBackupUrl();
}
