package org.openstack4j.openstack.storage.block.internal;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.api.storage.BlockVolumeBackupService;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeBackup;
import org.openstack4j.model.storage.block.VolumeBackupCreate;
import org.openstack4j.model.storage.block.VolumeBackupRestore;
import org.openstack4j.openstack.storage.block.domain.CinderVolumeBackup;
import org.openstack4j.openstack.storage.block.domain.CinderVolumeBackup.VolumeBackups;
import org.openstack4j.openstack.storage.block.domain.CinderVolumeBackupRestore;
import org.openstack4j.model.storage.block.options.BackupListOptions;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.openstack4j.model.storage.block.BackupRecord;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.storage.block.domain.CinderBackupRecord;

/**
 * OpenStack (Cinder) Volume Backup Operations API Implementation.
 *
 * @author Huang Yi
 */
public class BlockVolumeBackupServiceImpl extends BaseBlockStorageServices implements BlockVolumeBackupService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends VolumeBackup> list() {
        return get(VolumeBackups.class, uri("/backups/detail")).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends VolumeBackup> list(Map<String, String> filteringParams) {
        Invocation<VolumeBackups> invocation = buildInvocation(filteringParams);
        return invocation.execute().getList();
    }

    private Invocation<VolumeBackups> buildInvocation(Map<String, String> filteringParams) {
        Invocation<VolumeBackups> invocation = get(VolumeBackups.class, "/backups/detail");
        if (filteringParams == null) {
            return invocation;
        } else {
            for (Map.Entry<String, String> entry : filteringParams.entrySet()) {
                invocation = invocation.param(entry.getKey(), entry.getValue());
            }
        }
        return invocation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VolumeBackup get(String backupId) {
        Objects.requireNonNull(backupId);
        return get(CinderVolumeBackup.class, uri("/backups/%s", backupId)).execute();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse delete(String backupId) {
        Objects.requireNonNull(backupId);
        return deleteWithResponse(uri("/backups/%s", backupId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VolumeBackup create(VolumeBackupCreate vbc) {
        Objects.requireNonNull(vbc);
        Objects.requireNonNull(vbc.getVolumeId());
        if (vbc.getMetadata() != null)
            requireMicroVersion("Backup create option metadata", V(43));
        if (vbc.getAvailabilityZone() != null)
            requireMicroVersion("Backup create option availability_zone", V(51));
        return post(CinderVolumeBackup.class, uri("/backups")).entity(vbc).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VolumeBackupRestore restore(String backupId, String name, String volumeId) {
        _VolumeBackupRestore entity = new _VolumeBackupRestore(name, volumeId);
        return post(CinderVolumeBackupRestore.class, uri("/backups/%s/restore", backupId)).entity(entity).execute();
    }

    @JsonRootName("restore")
    private static class _VolumeBackupRestore implements ModelEntity {

        private static final long serialVersionUID = 1L;
        @JsonProperty("name")
        private String name;
        @JsonProperty("volume_id")
        private String volumeId;

        public _VolumeBackupRestore(String name, String volumeId) {
            this.name = name;
            this.volumeId = volumeId;
        }
    }

    @Override
    public List<? extends VolumeBackup> list(BackupListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Backup list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(VolumeBackups.class, uri("/backups/detail")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public VolumeBackup update(String backupId, String name, String description) {
        return update(backupId, name, description, null);
    }

    @Override
    public VolumeBackup update(String backupId, String name, String description, Map<String, String> metadata) {
        Objects.requireNonNull(backupId);
        requireMicroVersion("Backup update", V(9));
        if (metadata != null)
            requireMicroVersion("Backup metadata update", V(43));
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (metadata != null) body.put("metadata", metadata);
        return put(CinderVolumeBackup.class, uri("/backups/%s", backupId)).entity(JsonBody.of("backup", body)).execute();
    }

    @Override
    public BackupRecord exportRecord(String backupId) {
        Objects.requireNonNull(backupId);
        return get(CinderBackupRecord.class, uri("/backups/%s/export_record", backupId)).execute();
    }

    @Override
    public VolumeBackup importRecord(String backupService, String backupUrl) {
        Objects.requireNonNull(backupService);
        Objects.requireNonNull(backupUrl);
        return post(CinderVolumeBackup.class, uri("/backups/import_record")).entity(new CinderBackupRecord(backupService, backupUrl)).execute();
    }

    @Override public ActionResponse forceDelete(String backupId) { return action(Objects.requireNonNull(backupId), "os-force_delete", Collections.emptyMap()); }
    @Override public ActionResponse resetStatus(String backupId, String status) { return action(Objects.requireNonNull(backupId), "os-reset_status", Collections.singletonMap("status", Objects.requireNonNull(status))); }

    private ActionResponse action(String backupId, String action, Map<String, ?> body) {
        return post(ActionResponse.class, uri("/backups/%s/action", backupId)).entity(JsonBody.of(action, body)).execute();
    }
}
