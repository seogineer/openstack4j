package org.openstack4j.api.compute.microversion;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.actions.BackupOptions;
import org.openstack4j.model.compute.actions.EvacuateRequest;
import org.openstack4j.model.compute.actions.LiveMigrateRequest;
import org.openstack4j.model.compute.actions.RebuildRequest;
import org.openstack4j.model.compute.actions.RescueRequest;
import org.openstack4j.model.compute.actions.UnshelveRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServerActions")
public class ServerActionTests extends AbstractComputeMicroVersionTest {

    private JsonNode sentAction(String expectedVersion) throws Exception {
        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/servers/" + SERVER + "/action"), request.getPath());
        if (expectedVersion != null)
            assertVersionHeaders(request, expectedVersion);
        return body(request);
    }

    public void lockWithReason() throws Exception {
        negotiate("2.100");
        respondWith(202);
        Assert.assertTrue(osv3().compute().servers().lock(SERVER, "maintenance").isSuccess());
        Assert.assertEquals(sentAction("2.100").get("lock").get("locked_reason").asText(), "maintenance");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.73.*")
    public void lockWithReasonNeeds273() throws Exception {
        try {
            osv3().compute().servers().lock(SERVER, "x");
        } finally {
            assertNoMoreRequests();
        }
    }

    public void migrateToHost() throws Exception {
        negotiate("2.100");
        respondWith(202);
        osv3().compute().servers().migrateServer(SERVER, "compute-2");
        Assert.assertEquals(sentAction("2.100").get("migrate").get("host").asText(), "compute-2");
    }

    public void liveMigrateDefaultsToAutoAndForceIsCapped() throws Exception {
        negotiate("2.100");
        respondWith(202);
        respondWith(202);

        osv3().compute().servers().liveMigrate(SERVER, LiveMigrateRequest.create().host("compute-2"));
        JsonNode auto = sentAction("2.100").get("os-migrateLive");
        Assert.assertEquals(auto.get("block_migration").asText(), "auto");
        Assert.assertEquals(auto.get("host").asText(), "compute-2");
        Assert.assertFalse(auto.has("disk_over_commit"));

        osv3().compute().servers().liveMigrate(SERVER, LiveMigrateRequest.create().host("compute-2").force(true));
        Assert.assertTrue(sentAction("2.67").get("os-migrateLive").get("force").asBoolean());
    }

    public void liveMigrateDiskOverCommitIsCappedAt224() throws Exception {
        negotiate("2.100");
        respondWith(202);
        osv3().compute().servers().liveMigrate(SERVER, LiveMigrateRequest.create().blockMigration(false).diskOverCommit(false));
        JsonNode body = sentAction("2.24").get("os-migrateLive");
        Assert.assertFalse(body.get("block_migration").asBoolean());
        Assert.assertFalse(body.get("disk_over_commit").asBoolean());
        Assert.assertTrue(body.get("host").isNull());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*combined.*")
    public void liveMigrateAutoWithDiskOverCommitIsRejected() throws Exception {
        negotiate("2.100");
        try {
            osv3().compute().servers().liveMigrate(SERVER, LiveMigrateRequest.create().blockMigrationAuto().diskOverCommit(true));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void evacuateWithoutSharedStorage() throws Exception {
        negotiate("2.100");
        respondWith(200, "{}");
        osv3().compute().servers().evacuate(SERVER, EvacuateRequest.create().host("compute-2").adminPass("pw"));
        JsonNode body = sentAction("2.100").get("evacuate");
        Assert.assertEquals(body.get("host").asText(), "compute-2");
        Assert.assertEquals(body.get("adminPass").asText(), "pw");
        Assert.assertFalse(body.has("onSharedStorage"));
    }

    public void rebuildWithNewFields() throws Exception {
        negotiate("2.100");
        respondWith(202, "{\"server\": {\"id\": \"" + SERVER + "\"}}");
        osv3().compute().servers().rebuild(SERVER, RebuildRequest.create("img-2").name("vm-2").keyName("k1").userData("dXNlcg==")
                .hostname("vm-2").description("rebuilt").trustedImageCertificates(Collections.singletonList("c1")).preserveEphemeral(true));
        JsonNode body = sentAction("2.100").get("rebuild");
        Assert.assertEquals(body.get("imageRef").asText(), "img-2");
        Assert.assertEquals(body.get("key_name").asText(), "k1");
        Assert.assertEquals(body.get("user_data").asText(), "dXNlcg==");
        Assert.assertEquals(body.get("hostname").asText(), "vm-2");
        Assert.assertEquals(body.get("description").asText(), "rebuilt");
        Assert.assertEquals(body.get("trusted_image_certificates").get(0).asText(), "c1");
        Assert.assertTrue(body.get("preserve_ephemeral").asBoolean());
    }

    public void rebuildRemoveKeyNameSendsNull() throws Exception {
        negotiate("2.100");
        respondWith(202, "{\"server\": {\"id\": \"" + SERVER + "\"}}");
        osv3().compute().servers().rebuild(SERVER, RebuildRequest.create("img-2").removeKeyName());
        JsonNode body = sentAction("2.100").get("rebuild");
        Assert.assertTrue(body.has("key_name"));
        Assert.assertTrue(body.get("key_name").isNull());
    }

    public void unshelveToZoneAndHostAndUnpin() throws Exception {
        negotiate("2.100");
        respondWith(202);
        respondWith(202);
        osv3().compute().servers().unshelve(SERVER, UnshelveRequest.create().availabilityZone("az2").host("compute-3"));
        JsonNode first = sentAction("2.100").get("unshelve");
        Assert.assertEquals(first.get("availability_zone").asText(), "az2");
        Assert.assertEquals(first.get("host").asText(), "compute-3");

        osv3().compute().servers().unshelve(SERVER, UnshelveRequest.create().unpinAvailabilityZone());
        JsonNode second = sentAction("2.100").get("unshelve");
        Assert.assertTrue(second.has("availability_zone"));
        Assert.assertTrue(second.get("availability_zone").isNull());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.91.*")
    public void unshelveHostNeeds291() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.80");
        try {
            osv3().compute().servers().unshelve(SERVER, UnshelveRequest.create().host("compute-3"));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void rescueWithImage() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"adminPass\": \"pw\"}");
        osv3().compute().servers().rescue(SERVER, RescueRequest.create().rescueImageRef("img-r").adminPass("pw"));
        JsonNode body = sentAction("2.100").get("rescue");
        Assert.assertEquals(body.get("rescue_image_ref").asText(), "img-r");
        Assert.assertEquals(body.get("adminPass").asText(), "pw");
    }

    public void createBackupReturnsImageIdFromBody() throws Exception {
        negotiate("2.100");
        respondWith(202, "{\"image_id\": \"bk-1\"}");
        String id = osv3().compute().servers().createBackup(SERVER, BackupOptions.create("daily").daily(2));
        Assert.assertEquals(id, "bk-1");
        Assert.assertEquals(sentAction("2.100").get("createBackup").get("rotation").asInt(), 2);
    }
}
