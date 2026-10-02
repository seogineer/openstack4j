package org.openstack4j.api.compute.microversion;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ServerMigration;
import org.openstack4j.model.compute.ext.MigrationListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/MigrationMicroVersion")
public class MigrationMicroVersionTests extends AbstractComputeMicroVersionTest {

    private static final String MIGRATION = "{\"id\": 4, \"uuid\": \"12341d4b-346a-40d0-83c6-5f4f6892b650\", \"server_uuid\": \"" + SERVER + "\","
            + " \"status\": \"running\", \"source_compute\": \"compute1\", \"source_node\": \"node1\", \"dest_compute\": \"compute2\","
            + " \"dest_node\": \"node2\", \"dest_host\": \"1.2.3.4\", \"memory_total_bytes\": 123456, \"memory_processed_bytes\": 12345,"
            + " \"memory_remaining_bytes\": 111111, \"disk_total_bytes\": 234567, \"disk_processed_bytes\": 23456,"
            + " \"disk_remaining_bytes\": 211111, \"created_at\": \"2016-01-29T13:42:02.000000\", \"updated_at\": \"2016-01-29T13:42:02.000000\","
            + " \"user_id\": \"8dbaa0f0-ab95-4ffe-8cb4-9c89d2ac9d24\", \"project_id\": \"5f705771-3aa9-4f4c-8660-0d9522ffdbea\"}";

    public void listAndGetServerMigrations() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"migrations\": [" + MIGRATION + "]}");
        respondWith(200, "{\"migration\": " + MIGRATION + "}");

        List<? extends ServerMigration> list = osv3().compute().servers().migrations(SERVER);
        ServerMigration one = osv3().compute().servers().migration(SERVER, "4");

        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/migrations"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/migrations/4"));
        Assert.assertEquals(list.get(0).getId(), "4");
        Assert.assertEquals(list.get(0).getUuid(), "12341d4b-346a-40d0-83c6-5f4f6892b650");
        Assert.assertEquals(one.getMemoryRemainingBytes(), Long.valueOf(111111));
        Assert.assertEquals(one.getProjectId(), "5f705771-3aa9-4f4c-8660-0d9522ffdbea");
        Assert.assertNotNull(one.getCreatedAt());
    }

    public void forceCompleteAndAbort() throws Exception {
        negotiate("2.100");
        respondWith(202);
        respondWith(202);

        Assert.assertTrue(osv3().compute().servers().forceCompleteMigration(SERVER, "4").isSuccess());
        Assert.assertTrue(osv3().compute().servers().abortMigration(SERVER, "4").isSuccess());

        RecordedRequest force = takeRequest();
        Assert.assertTrue(force.getPath().endsWith("/servers/" + SERVER + "/migrations/4/action"));
        Assert.assertTrue(body(force).has("force_complete"));
        Assert.assertTrue(body(force).get("force_complete").isNull());
        RecordedRequest abort = takeRequest();
        Assert.assertEquals(abort.getMethod(), "DELETE");
        Assert.assertTrue(abort.getPath().endsWith("/servers/" + SERVER + "/migrations/4"));
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.24.*")
    public void abortNeeds224() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.23");
        try {
            osv3().compute().servers().abortMigration(SERVER, "4");
        } finally {
            assertNoMoreRequests();
        }
    }

    public void osMigrationsWithOptions() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"migrations\": []}");

        osv3().compute().migrations().list(MigrationListOptions.create().host("compute1").limit(10).marker("m-1").userId("u1").projectId("p1").migrationType("live-migration"));

        String path = takeRequest().getPath();
        Assert.assertTrue(path.contains("/os-migrations?"), path);
        for (String part : new String[] {"host=compute1", "limit=10", "marker=m-1", "user_id=u1", "project_id=p1", "migration_type=live-migration"})
            Assert.assertTrue(path.contains(part), path + " lacks " + part);
        Assert.assertEquals(MigrationListOptions.create().userId("u").getRequiredMicroVersion(), "2.80");
        Assert.assertEquals(MigrationListOptions.create().limit(1).getRequiredMicroVersion(), "2.59");
        Assert.assertNull(MigrationListOptions.create().host("h").getRequiredMicroVersion());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.59.*")
    public void osMigrationsPagingNeeds259() throws Exception {
        try {
            osv3().compute().migrations().list(MigrationListOptions.create().limit(1));
        } finally {
            assertNoMoreRequests();
        }
    }
}
