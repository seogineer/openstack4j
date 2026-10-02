package org.openstack4j.openstack.storage.block.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.storage.block.BackupRecord;

@JsonRootName("backup-record")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderBackupRecord implements BackupRecord {

    private static final long serialVersionUID = 1L;

    @JsonProperty("backup_service") private String backupService;
    @JsonProperty("backup_url") private String backupUrl;

    public CinderBackupRecord() {
    }

    public CinderBackupRecord(String backupService, String backupUrl) {
        this.backupService = backupService;
        this.backupUrl = backupUrl;
    }

    @Override public String getBackupService() { return backupService; }
    @Override public String getBackupUrl() { return backupUrl; }
}
