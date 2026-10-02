package org.openstack4j.api.storage.microversion;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.BackendCapabilities;
import org.openstack4j.model.storage.block.BlockExtension;
import org.openstack4j.model.storage.block.BlockLimits;
import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.ResourceFilter;
import org.openstack4j.model.storage.block.ServiceLogLevel;
import org.openstack4j.model.storage.block.StorageCluster;
import org.openstack4j.model.storage.block.StorageHost;
import org.openstack4j.model.storage.block.StorageHostResource;
import org.openstack4j.model.storage.block.WorkerCleanup;
import org.openstack4j.model.storage.block.options.ClusterListOptions;
import org.openstack4j.model.storage.block.options.PoolListOptions;
import org.openstack4j.model.storage.block.options.WorkerCleanupRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/Admin")
public class AdminServiceTests extends AbstractBlockStorageMicroVersionTest {

    public void clusters() throws Exception {
        negotiate("3.71");
        String cluster = "{\"cluster\": {\"binary\": \"cinder-volume\", \"created_at\": \"2026-06-01T02:46:28.000000\", \"disabled_reason\": null, \"last_heartbeat\": \"2026-06-01T02:46:28.000000\","
                + " \"name\": \"cluster_name\", \"num_down_hosts\": 0, \"num_hosts\": 2, \"state\": \"up\", \"status\": \"enabled\", \"updated_at\": \"2026-06-01T02:46:28.000000\","
                + " \"replication_status\": \"enabled\", \"frozen\": false, \"active_backend_id\": \"b1\"}}";
        respondWith(200, "{\"clusters\": [" + cluster.substring(12, cluster.length() - 1) + ", {\"name\": \"cluster2\", \"binary\": \"cinder-volume\", \"state\": \"down\", \"status\": \"disabled\", \"last_heartbeat\": \"\", \"num_hosts\": 2, \"num_down_hosts\": 1}]}");
        respondWith(200, cluster);
        respondWith(200, "{\"cluster\": {\"name\": \"cluster_name\", \"state\": \"up\", \"binary\": \"cinder-volume\", \"status\": \"disabled\", \"disabled_reason\": \"for testing\"}}");
        respondWith(200, "{\"cluster\": {\"name\": \"cluster_name\", \"state\": \"up\", \"binary\": \"cinder-volume\", \"status\": \"enabled\", \"disabled_reason\": null}}");

        List<? extends StorageCluster> all = osv3().blockStorage().clusters().listDetail(ClusterListOptions.create().isUp(true).frozen(false));
        StorageCluster one = osv3().blockStorage().clusters().get("cluster_name", null);
        StorageCluster disabled = osv3().blockStorage().clusters().disable("cluster_name", "cinder-volume", "for testing");
        StorageCluster enabled = osv3().blockStorage().clusters().enable("cluster_name", null);

        String list = takeRequest().getPath();
        Assert.assertTrue(list.contains("/clusters/detail?") && list.contains("is_up=true") && list.contains("frozen=false"), list);
        Assert.assertTrue(takeRequest().getPath().endsWith("/clusters/cluster_name?binary=cinder-volume"));
        RecordedRequest disable = takeRequest();
        Assert.assertEquals(disable.getMethod(), "PUT");
        Assert.assertTrue(disable.getPath().endsWith("/clusters/disable"));
        JsonNode body = body(disable);
        Assert.assertEquals(body.get("name").asText(), "cluster_name");
        Assert.assertEquals(body.get("disabled_reason").asText(), "for testing");
        Assert.assertTrue(takeRequest().getPath().endsWith("/clusters/enable"));
        Assert.assertEquals(all.get(0).getNumHosts(), Integer.valueOf(2));
        Assert.assertNull(all.get(1).getLastHeartbeat());     // Cinder sends "" for a cluster that never reported
        Assert.assertEquals(all.get(1).getNumDownHosts(), Integer.valueOf(1));
        Assert.assertEquals(one.getActiveBackendId(), "b1");
        Assert.assertEquals(one.getFrozen(), Boolean.FALSE);
        Assert.assertEquals(disabled.getStatus(), "disabled");
        Assert.assertEquals(enabled.getStatus(), "enabled");
        Assert.assertEquals(ClusterListOptions.create().frozen(true).getRequiredMicroVersion(), "3.26");
        Assert.assertNull(ClusterListOptions.create().name("x").getRequiredMicroVersion());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.7.*")
    public void clustersNeed37() throws Exception {
        try {
            osv3().blockStorage().clusters().list();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void serviceActions() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"binary\": \"cinder-volume\", \"host\": \"storage-1\", \"status\": \"enabled\"}");
        respondWith(200, "{\"binary\": \"cinder-volume\", \"disabled\": true, \"disabled_reason\": \"test2\", \"host\": \"storage-1\", \"status\": \"disabled\"}");
        respondWith(200);
        respondWith(200);
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"log_levels\": [{\"binary\": \"cinder-volume\", \"host\": \"storage-1\", \"levels\": {\"cinder.volume.api\": \"DEBUG\"}}]}");
        respondWith(202);

        osv3().blockStorage().services().enable("storage-1", "cinder-volume");
        osv3().blockStorage().services().disableWithReason("storage-1", "cinder-volume", "test2");
        osv3().blockStorage().services().freeze("storage-1");
        osv3().blockStorage().services().thaw("storage-1");
        osv3().blockStorage().services().failoverHost("storage-1", "b2");
        osv3().blockStorage().services().failover(null, "cluster1", "b2");
        List<? extends ServiceLogLevel> levels = osv3().blockStorage().services().getLog("cinder-volume", "storage-1", "cinder.volume");
        osv3().blockStorage().services().setLog("ERROR", "cinder-volume", "storage-1", "cinder.volume");

        RecordedRequest enable = takeRequest();
        Assert.assertEquals(enable.getMethod(), "PUT");
        Assert.assertTrue(enable.getPath().endsWith("/os-services/enable"));
        Assert.assertEquals(body(enable).get("host").asText(), "storage-1");
        RecordedRequest disable = takeRequest();
        Assert.assertTrue(disable.getPath().endsWith("/os-services/disable-log-reason"));
        Assert.assertEquals(body(disable).get("disabled_reason").asText(), "test2");
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-services/freeze"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-services/thaw"));
        RecordedRequest failoverHost = takeRequest();
        Assert.assertTrue(failoverHost.getPath().endsWith("/os-services/failover_host"));
        Assert.assertEquals(body(failoverHost).get("backend_id").asText(), "b2");
        RecordedRequest failover = takeRequest();
        Assert.assertTrue(failover.getPath().endsWith("/os-services/failover"));
        Assert.assertEquals(body(failover).get("cluster").asText(), "cluster1");
        Assert.assertFalse(body(failover).has("host"));
        RecordedRequest getLog = takeRequest();
        Assert.assertTrue(getLog.getPath().endsWith("/os-services/get-log"));
        Assert.assertEquals(body(getLog).get("prefix").asText(), "cinder.volume");
        RecordedRequest setLog = takeRequest();
        Assert.assertTrue(setLog.getPath().endsWith("/os-services/set-log"));
        Assert.assertEquals(body(setLog).get("level").asText(), "ERROR");
        Assert.assertEquals(levels.get(0).getLevels().get("cinder.volume.api"), "DEBUG");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.32.*")
    public void setLogNeeds332() throws Exception {
        try {
            osv3().blockStorage().services().setLog("DEBUG", null, null, null);
        } finally {
            assertNoMoreRequests();
        }
    }

    public void workersHostsCapabilitiesFiltersExtensions() throws Exception {
        negotiate("3.71");
        respondWith(202, "{\"cleaning\": [{\"id\": 1, \"host\": \"storage-1@lvm\", \"binary\": \"cinder-volume\", \"cluster_name\": \"test\"}], \"unavailable\": []}");
        respondWith(200, "{\"hosts\": [{\"service-status\": \"available\", \"service\": \"cinder-volume\", \"zone\": \"nova\", \"service-state\": \"enabled\", \"host_name\": \"storage-1@lvm\", \"last-update\": \"2026-10-02T21:38:35.000000\"}]}");
        respondWith(200, "{\"host\": [{\"resource\": {\"volume_count\": \"8\", \"total_volume_gb\": \"11\", \"total_snapshot_gb\": \"1\", \"project\": \"(total)\", \"host\": \"storage-1@lvm\", \"snapshot_count\": \"1\"}}]}");
        respondWith(200, "{\"namespace\": \"OS::Storage::Capabilities::lvm\", \"vendor_name\": \"Open Source\", \"volume_backend_name\": \"lvm-1\", \"pool_name\": \"lvm-1\", \"driver_version\": \"3.0.0\","
                + " \"storage_protocol\": \"iSCSI\", \"display_name\": \"LVM\", \"description\": \"d\", \"visibility\": \"public\", \"replication_targets\": [], \"properties\": {\"compression\": {\"type\": \"boolean\"}}}");
        respondWith(200, "{\"resource_filters\": [{\"filters\": [\"name\", \"status\"], \"resource\": \"volume\"}]}");
        respondWith(200, "{\"extensions\": [{\"name\": \"AdminActions\", \"alias\": \"os-admin-actions\", \"description\": \"Enable admin actions.\", \"updated\": \"2012-08-25T00:00:00+00:00\", \"links\": []}]}");

        WorkerCleanup cleanup = osv3().blockStorage().workers().cleanup(WorkerCleanupRequest.create().clusterName("test").binary("cinder-volume").isUp(true));
        List<? extends StorageHost> hosts = osv3().blockStorage().hosts().list();
        List<? extends StorageHostResource> resources = osv3().blockStorage().hosts().get("storage-1@lvm");
        BackendCapabilities caps = osv3().blockStorage().capabilities().get("storage-1@lvm");
        List<? extends ResourceFilter> filters = osv3().blockStorage().resourceFilters().list("volume");
        List<? extends BlockExtension> extensions = osv3().blockStorage().extensions().list();

        RecordedRequest work = takeRequest();
        Assert.assertTrue(work.getPath().endsWith("/workers/cleanup"));
        Assert.assertEquals(body(work).get("cluster_name").asText(), "test");
        Assert.assertTrue(body(work).get("is_up").asBoolean());
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-hosts"));
        Assert.assertTrue(java.net.URLDecoder.decode(takeRequest().getPath(), java.nio.charset.StandardCharsets.UTF_8).endsWith("/os-hosts/storage-1@lvm"));
        Assert.assertTrue(takeRequest().getPath().contains("/capabilities/storage-1"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/resource_filters?resource=volume"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/extensions"));
        Assert.assertEquals(cleanup.getCleaning().get(0).getClusterName(), "test");
        Assert.assertTrue(cleanup.getUnavailable().isEmpty());
        Assert.assertEquals(hosts.get(0).getServiceState(), "enabled");
        Assert.assertEquals(resources.get(0).getVolumeCount(), "8");
        Assert.assertEquals(caps.getStorageProtocol(), "iSCSI");
        Assert.assertTrue(caps.getProperties().containsKey("compression"));
        Assert.assertEquals(filters.get(0).getFilters().get(1), "status");
        Assert.assertEquals(extensions.get(0).getAlias(), "os-admin-actions");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.24.*")
    public void workersNeed324() throws Exception {
        try {
            osv3().blockStorage().workers().cleanup(WorkerCleanupRequest.create());
        } finally {
            assertNoMoreRequests();
        }
    }

    public void limitsQuotaClassAndPools() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"limits\": {\"rate\": [], \"absolute\": {\"maxTotalVolumes\": 10, \"totalVolumesUsed\": 1}}}");
        respondWith(200, "{\"quota_class_set\": {\"backup_gigabytes\": 1000, \"backups\": 10, \"gigabytes\": 1000, \"gigabytes___DEFAULT__\": -1, \"groups\": 10, \"id\": \"default\", \"per_volume_gigabytes\": -1, \"snapshots\": 10, \"snapshots___DEFAULT__\": -1, \"volumes\": 10, \"volumes___DEFAULT__\": -1}}");
        respondWith(200, "{\"quota_class_set\": {\"backups\": 20, \"gigabytes\": 1000, \"snapshots\": 10, \"volumes\": 10}}");
        respondWith(200, "{\"pools\": [{\"name\": \"storage-1@lvm-1#lvm-1\", \"capabilities\": {\"total_capacity_gb\": 100, \"free_capacity_gb\": 50, \"volume_backend_name\": \"lvm-1\"}}]}");

        BlockLimits limits = osv3().blockStorage().getLimits("p1");
        BlockQuotaSet quotaClass = osv3().blockStorage().quotaSets().quotaClass("default");
        BlockQuotaSet updated = osv3().blockStorage().quotaSets().updateQuotaClass("default", Builders.blockQuotaSet().backups(20).build());
        osv3().blockStorage().schedulerStatsPools().poolsDetail(PoolListOptions.create().volumeType("lvm").capability("QoS_support", "true"));

        Assert.assertTrue(takeRequest().getPath().endsWith("/limits?project_id=p1"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-quota-class-sets/default"));
        RecordedRequest put = takeRequest();
        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertEquals(body(put).get("quota_class_set").get("backups").asInt(), 20);
        String pools = takeRequest().getPath();
        Assert.assertTrue(pools.contains("/scheduler-stats/get_pools?") && pools.contains("detail=true") && pools.contains("volume_type=lvm") && pools.contains("QoS_support=true"), pools);
        Assert.assertEquals(limits.getAbsolute().getMaxTotalVolumes(), 10);
        Assert.assertEquals(quotaClass.getBackupGigabytes(), Integer.valueOf(1000));
        Assert.assertEquals(quotaClass.getGroups(), Integer.valueOf(10));
        Assert.assertEquals(quotaClass.getVolumeTypesQuotas().get("gigabytes___DEFAULT__"), Integer.valueOf(-1));
        Assert.assertFalse(quotaClass.getVolumeTypesQuotas().containsKey("backup_gigabytes"));
        Assert.assertEquals(updated.getBackups(), Integer.valueOf(20));
        Assert.assertEquals(PoolListOptions.create().volumeType("x").getRequiredMicroVersion(), "3.35");
        Assert.assertEquals(PoolListOptions.create().capability("a", "b").getRequiredMicroVersion(), "3.28");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.39.*")
    public void limitsByProjectNeeds339() throws Exception {
        try {
            osv3().blockStorage().getLimits("p1");
        } finally {
            assertNoMoreRequests();
        }
    }
}
