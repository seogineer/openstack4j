package org.openstack4j.api.storage.microversion;

import java.util.List;

import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeBackup;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.VolumeTransfer;
import org.openstack4j.model.storage.block.VolumeType;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/Models")
public class BlockStorageModelTests extends AbstractBlockStorageMicroVersionTest {

    public void volumeReads371Fields() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/volume_3_71.json");

        Volume volume = osv3().blockStorage().volumes().get(VOLUME);
        takeRequest();

        Assert.assertEquals(volume.getUserId(), "e365357fdf4d4a37a07fde3209cac4aa");
        Assert.assertNotNull(volume.getUpdatedAt());
        Assert.assertEquals(volume.getGroupId(), "0b1c9f2e-6a7b-4d8e-9f00-1a2b3c4d5e6f");
        Assert.assertEquals(volume.getServiceUuid(), "1afa4698-dd4e-4467-a7bf-5f891f03b634");
        Assert.assertEquals(volume.getVolumeTypeId(), "c23142c0-9883-4224-beb8-06d7c231be75");
        Assert.assertEquals(volume.getConsumesQuota(), Boolean.TRUE);
        Assert.assertNull(volume.getClusterName());
        Assert.assertNull(volume.getEncryptionKeyId());
        Assert.assertNull(volume.getProviderId());
        Assert.assertEquals(volume.getLinks().size(), 2);
        Assert.assertEquals(volume.getVolumeType(), "__DEFAULT__");     // legacy getter untouched
        Assert.assertFalse(volume.bootable());
    }

    public void sharedTargetsNullStaysNull() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/volume_3_71.json");
        Volume volume = osv3().blockStorage().volumes().get(VOLUME);
        takeRequest();
        Assert.assertNull(volume.getSharedTargets());
    }

    public void snapshotReads371Fields() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/snapshot_3_71.json");

        VolumeSnapshot snapshot = osv3().blockStorage().snapshots().get(SNAPSHOT);
        takeRequest();

        Assert.assertEquals(snapshot.getUserId(), "e365357fdf4d4a37a07fde3209cac4aa");
        Assert.assertEquals(snapshot.getProjectId(), "2580a7b51d564c1d848ee27fda2db713");
        Assert.assertEquals(snapshot.getProgress(), "100%");
        Assert.assertNull(snapshot.getGroupSnapshotId());
        Assert.assertEquals(snapshot.getConsumesQuota(), Boolean.TRUE);
        Assert.assertNotNull(snapshot.getUpdatedAt());
    }

    public void backupReads356Fields() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/backup_3_56.json");

        VolumeBackup backup = osv3().blockStorage().backups().get("3052c307-119e-4f78-960e-972078aa15a8");
        takeRequest();

        Assert.assertEquals(backup.getProjectId(), "89afd400-b646-4bbc-b12b-c0a4d63e5bd3");
        Assert.assertEquals(backup.getUserId(), "c853ca26-e8ea-4797-8a52-ee124a013d0e");
        Assert.assertEquals(backup.getMetadata().get("key"), "value");
        Assert.assertEquals(backup.getEncryptionKeyId(), "8ef3e7a1-1a2b-4c3d-9e8f-0a1b2c3d4e5f");
        Assert.assertNotNull(backup.getDataTimestamp());
        Assert.assertNotNull(backup.getUpdatedAt());
        Assert.assertEquals(backup.getLinks().size(), 1);
        Assert.assertEquals(backup.getHasDependentBackups(), Boolean.FALSE);
    }

    public void volumeTypeReadsPublicDescriptionAndQos() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/types_3_71.json");

        List<? extends VolumeType> types = osv3().blockStorage().volumes().listVolumeTypes();
        takeRequest();

        Assert.assertEquals(types.get(0).getDescription(), "Default Volume Type");
        Assert.assertEquals(types.get(0).isPublic(), Boolean.TRUE);
        Assert.assertEquals(types.get(0).getAccessIsPublic(), Boolean.TRUE);
        Assert.assertNull(types.get(0).getQosSpecsId());
    }

    public void transferReads357Fields() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/transfer_3_57.json");

        VolumeTransfer transfer = osv3().blockStorage().volumes().transfer().get("94bae1a0-83fb-496c-9cd2-800d8237ab0d");
        takeRequest();

        Assert.assertEquals(transfer.getAccepted(), Boolean.FALSE);
        Assert.assertEquals(transfer.getNoSnapshots(), Boolean.FALSE);
        Assert.assertEquals(transfer.getSourceProjectId(), "89afd400-b646-4bbc-b12b-c0a4d63e5bd3");
        Assert.assertNull(transfer.getDestinationProjectId());
    }

    public void serviceReadsClusterAndReplicationFields() throws Exception {
        negotiate("3.71");
        respondWith("/storage/microversion/services_3_49.json");

        List<? extends org.openstack4j.model.storage.block.ext.Service> services = osv3().blockStorage().services().list();
        takeRequest();

        Assert.assertNull(services.get(0).getCluster());
        Assert.assertEquals(services.get(1).getCluster(), "cluster1");
        Assert.assertEquals(services.get(1).getFrozen(), Boolean.FALSE);
        Assert.assertEquals(services.get(1).getReplicationStatus(), "disabled");
        Assert.assertEquals(services.get(1).getBackendState(), "up");
        Assert.assertNull(services.get(1).getActiveBackendId());
    }

    public void quotaSetReadsNewFieldsAndPerTypeKeys() throws Exception {
        respondWith("/storage/microversion/quota_set.json");

        BlockQuotaSet quota = osv3().blockStorage().quotaSets().get("fake_tenant");
        takeRequest();

        Assert.assertEquals(quota.getBackups(), Integer.valueOf(10));
        Assert.assertEquals(quota.getBackupGigabytes(), Integer.valueOf(1000));
        Assert.assertEquals(quota.getPerVolumeGigabytes(), Integer.valueOf(-1));
        Assert.assertEquals(quota.getGroups(), Integer.valueOf(10));
        Assert.assertEquals(quota.getVolumeTypesQuotas().get("volumes___DEFAULT__"), Integer.valueOf(-1));
        Assert.assertFalse(quota.getVolumeTypesQuotas().containsKey("backup_gigabytes"));
        Assert.assertFalse(quota.getVolumeTypesQuotas().containsKey("per_volume_gigabytes"));
    }
}
