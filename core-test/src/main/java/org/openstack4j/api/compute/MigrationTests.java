package org.openstack4j.api.compute;

import java.util.List;

import org.openstack4j.api.AbstractTest;
import org.openstack4j.model.compute.ext.Migration;
import org.openstack4j.model.compute.ext.Migration.Status;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Migrations based Test Cases
 *
 * @author Jeremy Unruh
 */
@Test(suiteName = "Migrations", enabled = true)
public class MigrationTests extends AbstractTest {

    private static final String JSON_MIGRATIONS = "/compute/migrations.json";

    public void listMigrations() throws Exception {
        respondWith(JSON_MIGRATIONS);
        List<? extends Migration> migrations = osv3().compute().migrations().list();

        assertEquals(2, migrations.size());
        assertEquals(Status.DONE, migrations.get(0).getStatus());
        assertEquals(Status.CONFIRMED, migrations.get(1).getStatus());
    }

    public void listMigrationsWithNewStatuses() throws Exception {
        respondWith("/compute/migrations_statuses.json");
        List<? extends Migration> migrations = osv3().compute().migrations().list();

        assertEquals(migrations.size(), 9);
        assertEquals(migrations.get(0).getStatus(), Status.PRE_MIGRATING);
        assertEquals(migrations.get(1).getStatus(), Status.POST_MIGRATING);
        assertEquals(migrations.get(2).getStatus(), Status.RUNNING);
        assertEquals(migrations.get(3).getStatus(), Status.QUEUED);
        assertEquals(migrations.get(4).getStatus(), Status.COMPLETED);
        assertEquals(migrations.get(5).getStatus(), Status.CANCELLED);
        assertEquals(migrations.get(6).getStatus(), Status.REVERTED);
        // unknown values keep falling back to MIGRATING
        assertEquals(migrations.get(7).getStatus(), Status.MIGRATING);
        assertEquals(migrations.get(8).getStatus(), Status.MIGRATING);
    }

    public void statusForValue() {
        assertEquals(Status.forValue(null), Status.MIGRATING);
        assertEquals(Status.forValue("Done"), Status.DONE);
        assertEquals(Status.forValue("pre_migrating"), Status.PRE_MIGRATING);
        assertEquals(Status.forValue("pre-migrating"), Status.PRE_MIGRATING);
        assertEquals(Status.forValue("bogus"), Status.MIGRATING);
    }

    @Override
    protected Service service() {
        return Service.COMPUTE;
    }

}
