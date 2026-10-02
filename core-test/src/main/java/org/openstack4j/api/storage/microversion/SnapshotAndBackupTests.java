package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.BackupRecord;
import org.openstack4j.model.storage.block.VolumeBackup;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/SnapshotsBackups")
public class SnapshotAndBackupTests extends AbstractBlockStorageMicroVersionTest {

    private static final String BACKUP = "3052c307-119e-4f78-960e-972078aa15a8";

    public void snapshotMetadataAndActions() throws Exception {
        respondWith(200, "{\"metadata\": {\"k\": \"v\"}}");
        respondWith(200, "{\"meta\": {\"k\": \"v2\"}}");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);

        Map<String, String> metadata = osv3().blockStorage().snapshots().metadata(SNAPSHOT);
        String updated = osv3().blockStorage().snapshots().updateMetadataItem(SNAPSHOT, "k", "v2");
        osv3().blockStorage().snapshots().resetStatus(SNAPSHOT, "available");
        osv3().blockStorage().snapshots().forceDelete(SNAPSHOT);
        osv3().blockStorage().snapshots().updateStatus(SNAPSHOT, "creating", "80%");
        osv3().blockStorage().snapshots().unmanage(SNAPSHOT);

        Assert.assertTrue(takeRequest().getPath().endsWith("/snapshots/" + SNAPSHOT + "/metadata"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/snapshots/" + SNAPSHOT + "/metadata/k"));
        Assert.assertEquals(body(takeRequest()).get("os-reset_status").get("status").asText(), "available");
        Assert.assertTrue(body(takeRequest()).has("os-force_delete"));
        JsonNode status = body(takeRequest()).get("os-update_snapshot_status");
        Assert.assertEquals(status.get("status").asText(), "creating");
        Assert.assertEquals(status.get("progress").asText(), "80%");
        Assert.assertTrue(body(takeRequest()).has("os-unmanage"));
        Assert.assertEquals(metadata.get("k"), "v");
        Assert.assertEquals(updated, "v2");
    }

    public void backupUpdateExportImportAndActions() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"backup\": {\"id\": \"" + BACKUP + "\", \"name\": \"renamed\", \"links\": []}}");
        respondWith(200, "{\"backup-record\": {\"backup_service\": \"cinder.backup.drivers.swift\", \"backup_url\": \"eyJzdGF0\"}}");
        respondWith(201, "{\"backup\": {\"id\": \"" + BACKUP + "\", \"name\": \"imported\", \"links\": []}}");
        respondWith(202);
        respondWith(202);

        VolumeBackup updated = osv3().blockStorage().backups().update(BACKUP, "renamed", null, Collections.singletonMap("k", "v"));
        BackupRecord record = osv3().blockStorage().backups().exportRecord(BACKUP);
        VolumeBackup imported = osv3().blockStorage().backups().importRecord(record.getBackupService(), record.getBackupUrl());
        osv3().blockStorage().backups().forceDelete(BACKUP);
        osv3().blockStorage().backups().resetStatus(BACKUP, "error");

        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        assertVersionHeader(update, "3.71");
        JsonNode body = body(update).get("backup");
        Assert.assertEquals(body.get("name").asText(), "renamed");
        Assert.assertFalse(body.has("description"));
        Assert.assertEquals(body.get("metadata").get("k").asText(), "v");
        Assert.assertTrue(takeRequest().getPath().endsWith("/backups/" + BACKUP + "/export_record"));
        RecordedRequest imp = takeRequest();
        Assert.assertTrue(imp.getPath().endsWith("/backups/import_record"));
        Assert.assertEquals(body(imp).get("backup-record").get("backup_url").asText(), "eyJzdGF0");
        Assert.assertTrue(body(takeRequest()).has("os-force_delete"));
        Assert.assertEquals(body(takeRequest()).get("os-reset_status").get("status").asText(), "error");
        Assert.assertEquals(updated.getName(), "renamed");
        Assert.assertEquals(imported.getName(), "imported");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.9.*")
    public void backupUpdateNeeds39() throws Exception {
        try {
            osv3().blockStorage().backups().update(BACKUP, "n", "d");
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.43.*")
    public void backupMetadataUpdateNeeds343() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.40");
        try {
            osv3().blockStorage().backups().update(BACKUP, "n", null, Collections.singletonMap("k", "v"));
        } finally {
            assertNoMoreRequests();
        }
    }
}
