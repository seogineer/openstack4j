package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.options.VolumeMigrateRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/VolumeActions")
public class VolumeActionTests extends AbstractBlockStorageMicroVersionTest {

    private JsonNode sentAction(String expectedVersion) throws Exception {
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/volumes/" + VOLUME + "/action"), request.getPath());
        if (expectedVersion != null)
            assertVersionHeader(request, expectedVersion);
        return body(request);
    }

    public void metadataCrud() throws Exception {
        respondWith(200, "{\"metadata\": {\"a\": \"1\"}}");
        respondWith(200, "{\"metadata\": {\"a\": \"1\", \"b\": \"2\"}}");
        respondWith(200, "{\"metadata\": {\"b\": \"2\"}}");
        respondWith(200, "{\"meta\": {\"b\": \"2\"}}");
        respondWith(200, "{\"meta\": {\"b\": \"3\"}}");
        respondWith(200);

        Map<String, String> all = osv3().blockStorage().volumes().metadata(VOLUME);
        Map<String, String> merged = osv3().blockStorage().volumes().setMetadata(VOLUME, Collections.singletonMap("b", "2"));
        Map<String, String> replaced = osv3().blockStorage().volumes().replaceMetadata(VOLUME, Collections.singletonMap("b", "2"));
        String item = osv3().blockStorage().volumes().metadataItem(VOLUME, "b");
        String updated = osv3().blockStorage().volumes().updateMetadataItem(VOLUME, "b", "3");
        boolean deleted = osv3().blockStorage().volumes().deleteMetadataItem(VOLUME, "b").isSuccess();

        Assert.assertEquals(takeRequest().getMethod(), "GET");
        RecordedRequest post = takeRequest();
        Assert.assertEquals(post.getMethod(), "POST");
        Assert.assertEquals(body(post).get("metadata").get("b").asText(), "2");
        Assert.assertEquals(takeRequest().getMethod(), "PUT");
        Assert.assertTrue(takeRequest().getPath().endsWith("/volumes/" + VOLUME + "/metadata/b"));
        RecordedRequest put = takeRequest();
        Assert.assertEquals(body(put).get("meta").get("b").asText(), "3");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(all.get("a"), "1");
        Assert.assertEquals(merged.size(), 2);
        Assert.assertEquals(replaced.size(), 1);
        Assert.assertEquals(item, "2");
        Assert.assertEquals(updated, "3");
        Assert.assertTrue(deleted);
    }

    public void imageMetadataActions() throws Exception {
        respondWith(200, "{\"metadata\": {\"image_name\": \"cirros\"}}");
        respondWith(200, "{\"metadata\": {\"image_name\": \"cirros\", \"kernel_id\": \"k\"}}");
        respondWith(200);

        Map<String, String> shown = osv3().blockStorage().volumes().imageMetadata(VOLUME);
        Map<String, String> set = osv3().blockStorage().volumes().setImageMetadata(VOLUME, Collections.singletonMap("kernel_id", "k"));
        osv3().blockStorage().volumes().unsetImageMetadata(VOLUME, "kernel_id");

        Assert.assertTrue(sentAction(null).has("os-show_image_metadata"));
        Assert.assertEquals(sentAction(null).get("os-set_image_metadata").get("metadata").get("kernel_id").asText(), "k");
        Assert.assertEquals(sentAction(null).get("os-unset_image_metadata").get("key").asText(), "kernel_id");
        Assert.assertEquals(shown.get("image_name"), "cirros");
        Assert.assertEquals(set.size(), 2);
    }

    public void revertReimageAndExtendCompletion() throws Exception {
        negotiate("3.71");
        respondWith(202);
        respondWith(202);
        respondWith(202);

        osv3().blockStorage().volumes().revertToSnapshot(VOLUME, SNAPSHOT);
        osv3().blockStorage().volumes().reimage(VOLUME, "img-1", false);
        osv3().blockStorage().volumes().completeExtend(VOLUME, false);

        Assert.assertEquals(sentAction("3.71").get("revert").get("snapshot_id").asText(), SNAPSHOT);
        JsonNode reimage = sentAction("3.71").get("os-reimage");
        Assert.assertEquals(reimage.get("image_id").asText(), "img-1");
        Assert.assertFalse(reimage.get("reimage_reserved").asBoolean());
        Assert.assertFalse(sentAction("3.71").get("os-extend_volume_completion").get("error").asBoolean());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.40.*")
    public void revertNeeds340() throws Exception {
        try {
            osv3().blockStorage().volumes().revertToSnapshot(VOLUME, SNAPSHOT);
        } finally {
            assertNoMoreRequests();
        }
    }

    public void retypeMigrateAndCompletion() throws Exception {
        negotiate("3.71");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);

        osv3().blockStorage().volumes().retype(VOLUME, "ssd", "on-demand");
        osv3().blockStorage().volumes().migrate(VOLUME, VolumeMigrateRequest.create().host("storage-2@lvm").forceHostCopy(true));
        osv3().blockStorage().volumes().migrate(VOLUME, VolumeMigrateRequest.create().cluster("cluster1").lockVolume(true));
        osv3().blockStorage().volumes().completeMigration(VOLUME, "new-vol", false);

        JsonNode retype = sentAction("3.71").get("os-retype");
        Assert.assertEquals(retype.get("new_type").asText(), "ssd");
        Assert.assertEquals(retype.get("migration_policy").asText(), "on-demand");
        JsonNode byHost = sentAction("3.71").get("os-migrate_volume");
        Assert.assertEquals(byHost.get("host").asText(), "storage-2@lvm");
        Assert.assertTrue(byHost.get("force_host_copy").asBoolean());
        Assert.assertFalse(byHost.has("cluster"));
        JsonNode byCluster = sentAction("3.71").get("os-migrate_volume");
        Assert.assertEquals(byCluster.get("cluster").asText(), "cluster1");
        Assert.assertTrue(byCluster.get("lock_volume").asBoolean());
        JsonNode completion = sentAction("3.71").get("os-migrate_volume_completion");
        Assert.assertEquals(completion.get("new_volume").asText(), "new-vol");
        Assert.assertFalse(completion.get("error").asBoolean());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.16.*")
    public void migrateToClusterNeeds316() throws Exception {
        try {
            osv3().blockStorage().volumes().migrate(VOLUME, VolumeMigrateRequest.create().cluster("c"));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void adminAndAttachWorkflowActions() throws Exception {
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"connection_info\": {\"driver_volume_type\": \"iscsi\", \"data\": {\"target_lun\": 1}}}");
        respondWith(202);
        respondWith(202);

        osv3().blockStorage().volumes().unmanage(VOLUME);
        osv3().blockStorage().volumes().reserve(VOLUME);
        osv3().blockStorage().volumes().unreserve(VOLUME);
        osv3().blockStorage().volumes().beginDetaching(VOLUME);
        osv3().blockStorage().volumes().rollDetaching(VOLUME);
        Map<String, Object> info = osv3().blockStorage().volumes().initializeConnection(VOLUME, Collections.singletonMap("initiator", "iqn.x"));
        osv3().blockStorage().volumes().terminateConnection(VOLUME, Collections.singletonMap("initiator", "iqn.x"));
        osv3().blockStorage().volumes().setStatus(VOLUME, "available", "detached", null);

        for (String action : new String[] {"os-unmanage", "os-reserve", "os-unreserve", "os-begin_detaching", "os-roll_detaching"})
            Assert.assertTrue(sentAction(null).has(action), action);
        Assert.assertEquals(sentAction(null).get("os-initialize_connection").get("connector").get("initiator").asText(), "iqn.x");
        Assert.assertTrue(sentAction(null).get("os-terminate_connection").has("connector"));
        JsonNode reset = sentAction(null).get("os-reset_status");
        Assert.assertEquals(reset.get("status").asText(), "available");
        Assert.assertEquals(reset.get("attach_status").asText(), "detached");
        Assert.assertFalse(reset.has("migration_status"));
        Assert.assertEquals(info.get("driver_volume_type"), "iscsi");
    }
}
