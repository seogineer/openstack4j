package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.ManageableSnapshot;
import org.openstack4j.model.storage.block.ManageableVolume;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeTransfer;
import org.openstack4j.model.storage.block.options.ManageableListOptions;
import org.openstack4j.model.storage.block.options.SnapshotManageRequest;
import org.openstack4j.model.storage.block.options.TransferListOptions;
import org.openstack4j.model.storage.block.options.VolumeManageRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/TransfersManage")
public class TransferAndManageTests extends AbstractBlockStorageMicroVersionTest {

    private static final String TRANSFER = "{\"transfer\": {\"accepted\": false, \"auth_key\": \"e2cb02466324813c\", \"created_at\": \"2026-06-12T21:21:38.392033\", \"destination_project_id\": null,"
            + " \"id\": \"94bae1a0-83fb-496c-9cd2-800d8237ab0d\", \"links\": [], \"name\": \"first volume\", \"no_snapshots\": true, \"source_project_id\": \"p1\", \"volume_id\": \"" + VOLUME + "\"}}";

    public void newTransferApi() throws Exception {
        negotiate("3.71");
        respondWith(202, TRANSFER);
        respondWith(200, "{\"transfers\": [" + TRANSFER.substring(13, TRANSFER.length() - 1) + "]}");
        respondWith(200, TRANSFER);
        respondWith(202, TRANSFER);
        respondWith(202);

        VolumeTransfer created = osv3().blockStorage().volumeTransfers().create(VOLUME, "first volume", true);
        List<? extends VolumeTransfer> all = osv3().blockStorage().volumeTransfers().listDetail(TransferListOptions.create().limit(10));
        osv3().blockStorage().volumeTransfers().get(created.getId());
        VolumeTransfer accepted = osv3().blockStorage().volumeTransfers().accept(created.getId(), "e2cb02466324813c");
        osv3().blockStorage().volumeTransfers().delete(created.getId());

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/volume-transfers"));
        JsonNode body = body(create).get("transfer");
        Assert.assertEquals(body.get("volume_id").asText(), VOLUME);
        Assert.assertTrue(body.get("no_snapshots").asBoolean());
        Assert.assertTrue(takeRequest().getPath().contains("/volume-transfers/detail?limit=10"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/volume-transfers/" + created.getId()));
        RecordedRequest accept = takeRequest();
        Assert.assertTrue(accept.getPath().endsWith("/volume-transfers/" + created.getId() + "/accept"));
        Assert.assertEquals(body(accept).get("accept").get("auth_key").asText(), "e2cb02466324813c");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getNoSnapshots(), Boolean.TRUE);
        Assert.assertEquals(all.get(0).getSourceProjectId(), "p1");
        Assert.assertEquals(accepted.getId(), created.getId());
        Assert.assertEquals(TransferListOptions.create().limit(1).getRequiredMicroVersion(), "3.59");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.55.*")
    public void newTransferApiNeeds355() throws Exception {
        try {
            osv3().blockStorage().volumeTransfers().list();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void legacyTransferApiUnchanged() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"transfers\": []}");
        osv3().blockStorage().volumes().transfer().list(true);
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/os-volume-transfer/detail"));
        assertVersionHeader(request, "3.71");
    }

    public void manageableVolumesAndSnapshots() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"manageable-volumes\": [{\"reference\": {\"source-name\": \"lvol0\"}, \"size\": 1, \"safe_to_manage\": true, \"reason_not_safe\": null, \"cinder_id\": null, \"extra_info\": null}]}");
        respondWith(202, "{\"volume\": {\"id\": \"" + VOLUME + "\", \"status\": \"creating\", \"name\": \"NewVolume\"}}");
        respondWith(200, "{\"manageable-snapshots\": [{\"reference\": {\"source-name\": \"lvol0-snap\"}, \"source_reference\": {\"source-name\": \"lvol0\"}, \"size\": 1, \"safe_to_manage\": true, \"reason_not_safe\": null, \"cinder_id\": null, \"extra_info\": null}]}");
        respondWith(202, "{\"snapshot\": {\"id\": \"" + SNAPSHOT + "\", \"status\": \"creating\", \"volume_id\": \"" + VOLUME + "\"}}");

        List<? extends ManageableVolume> volumes = osv3().blockStorage().manageableVolumes().listDetail(ManageableListOptions.create().host("storage-1@lvm-1"));
        Volume managed = osv3().blockStorage().manageableVolumes().manage(VolumeManageRequest.create(Collections.singletonMap("source-name", "lvol0"))
                .host("storage-1@lvm-1").name("NewVolume").bootable(true).metadata(Collections.singletonMap("k", "v")));
        List<? extends ManageableSnapshot> snapshots = osv3().blockStorage().manageableSnapshots().list(ManageableListOptions.create().cluster("cluster1"));
        osv3().blockStorage().manageableSnapshots().manage(SnapshotManageRequest.create(VOLUME, Collections.singletonMap("source-name", "lvol0-snap")).name("s"));

        Assert.assertTrue(java.net.URLDecoder.decode(takeRequest().getPath(), java.nio.charset.StandardCharsets.UTF_8).contains("/manageable_volumes/detail?host=storage-1@lvm-1"));
        RecordedRequest manage = takeRequest();
        Assert.assertTrue(manage.getPath().endsWith("/manageable_volumes"));
        JsonNode body = body(manage).get("volume");
        Assert.assertEquals(body.get("ref").get("source-name").asText(), "lvol0");
        Assert.assertEquals(body.get("host").asText(), "storage-1@lvm-1");
        Assert.assertTrue(body.get("bootable").asBoolean());
        Assert.assertEquals(body.get("metadata").get("k").asText(), "v");
        Assert.assertTrue(takeRequest().getPath().contains("/manageable_snapshots?cluster=cluster1"));
        RecordedRequest manageSnap = takeRequest();
        Assert.assertTrue(manageSnap.getPath().endsWith("/manageable_snapshots"));
        Assert.assertEquals(body(manageSnap).get("snapshot").get("volume_id").asText(), VOLUME);
        Assert.assertEquals(volumes.get(0).getReference().get("source-name"), "lvol0");
        Assert.assertEquals(volumes.get(0).getSafeToManage(), Boolean.TRUE);
        Assert.assertEquals(managed.getName(), "NewVolume");
        Assert.assertEquals(snapshots.get(0).getSourceReference().get("source-name"), "lvol0");
        Assert.assertEquals(ManageableListOptions.create().cluster("c").getRequiredMicroVersion(), "3.17");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.8.*")
    public void manageableNeeds38() throws Exception {
        try {
            osv3().blockStorage().manageableVolumes().list(ManageableListOptions.create().host("h"));
        } finally {
            assertNoMoreRequests();
        }
    }
}
