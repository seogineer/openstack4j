package org.openstack4j.api.compute.microversion;

import java.util.List;

import org.openstack4j.model.compute.HostAggregate;
import org.openstack4j.model.compute.InstanceAction;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.compute.ext.HypervisorListOptions;
import org.openstack4j.model.compute.ext.Migration;
import org.openstack4j.model.compute.ext.MigrationListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ModelFields")
public class ModelFieldTests extends AbstractComputeMicroVersionTest {

    public void aggregateUuid() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"aggregate\": {\"id\": 1, \"uuid\": \"fd0a5b12-7e8d-469d-bfd5-64a6823e7407\", \"name\": \"agg\", \"hosts\": [], \"metadata\": {}, \"deleted\": false}}");
        HostAggregate aggregate = osv3().compute().hostAggregates().get("1");
        takeRequest();
        Assert.assertEquals(aggregate.getUuid(), "fd0a5b12-7e8d-469d-bfd5-64a6823e7407");
    }

    public void instanceActionUpdatedAtAndEventDetails() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"instanceAction\": {\"action\": \"create\", \"instance_uuid\": \"" + SERVER + "\", \"request_id\": \"req-1\","
                + " \"start_time\": \"2026-09-22T08:02:43.000000\", \"updated_at\": \"2026-09-22T08:02:56.000000\", \"user_id\": \"u\", \"project_id\": \"p\","
                + " \"events\": [{\"event\": \"compute__do_build_and_run_instance\", \"start_time\": \"2026-09-22T08:02:44.000000\","
                + " \"finish_time\": \"2026-09-22T08:02:55.000000\", \"result\": \"Success\", \"host\": \"compute-1\", \"hostId\": \"abc\", \"details\": null}]}}");

        InstanceAction action = osv3().compute().servers().instanceActions().get(SERVER, "req-1");
        takeRequest();

        Assert.assertNotNull(action.getUpdatedAt());
        Assert.assertEquals(action.getEvents().size(), 1);
        Assert.assertEquals(action.getEvents().get(0).getHost(), "compute-1");
        Assert.assertEquals(action.getEvents().get(0).getHostId(), "abc");
        Assert.assertNull(action.getEvents().get(0).getDetails());
    }

    public void migrationFields() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"migrations\": [{\"id\": 1, \"uuid\": \"42341d4b-346a-40d0-83c6-5f4f6892b650\", \"migration_type\": \"live-migration\","
                + " \"status\": \"completed\", \"instance_uuid\": \"" + SERVER + "\", \"user_id\": \"u1\", \"project_id\": \"p1\","
                + " \"created_at\": \"2026-09-22T08:02:43.000000\", \"updated_at\": \"2026-09-22T08:02:56.000000\"}]}");

        List<? extends Migration> migrations = osv3().compute().migrations().list(MigrationListOptions.create().userId("u1"));
        takeRequest();

        Assert.assertEquals(migrations.get(0).getMigrationType(), "live-migration");
        Assert.assertEquals(migrations.get(0).getUuid(), "42341d4b-346a-40d0-83c6-5f4f6892b650");
        Assert.assertEquals(migrations.get(0).getProjectId(), "p1");
    }

    public void hypervisorServersAndUptime() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"hypervisors\": [{\"id\": \"c48f6247-abe4-4a24-824e-ea39e108874f\", \"hypervisor_hostname\": \"compute-1\", \"state\": \"up\","
                + " \"status\": \"enabled\", \"uptime\": \" 08:32:11 up 93 days\", \"servers\": [{\"uuid\": \"" + SERVER + "\", \"name\": \"instance-00000001\"}],"
                + " \"service\": {\"host\": \"compute-1\", \"id\": \"e81d66a4-ddd3-4aba-8a84-171d1cb4d339\", \"disabled_reason\": null}}]}");

        List<? extends Hypervisor> hypervisors = osv3().compute().hypervisors().list(HypervisorListOptions.create().withServers(true));
        takeRequest();

        Assert.assertEquals(hypervisors.get(0).getServers().get(0).getUuid(), SERVER);
        Assert.assertEquals(hypervisors.get(0).getUptime(), " 08:32:11 up 93 days");
        Assert.assertEquals(hypervisors.get(0).getVirtualCPU(), 0);
    }
}
