package org.openstack4j.api.compute.microversion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.AssistedVolumeSnapshot;
import org.openstack4j.model.compute.ConsoleConnectionInfo;
import org.openstack4j.model.compute.ExternalEvent;
import org.openstack4j.model.compute.ExternalEventCreate;
import org.openstack4j.model.compute.InstanceUsageAuditLog;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/NewServices")
public class NewComputeServicesTests extends AbstractComputeMicroVersionTest {

    public void externalEvents() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"events\": [{\"name\": \"network-changed\", \"server_uuid\": \"" + SERVER + "\", \"status\": \"completed\", \"code\": 200, \"tag\": \"port-1\"}]}");

        List<? extends ExternalEvent> events = osv3().compute().serverExternalEvents()
                .create(Collections.singletonList(ExternalEventCreate.of("network-changed", SERVER).tag("port-1")));

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/os-server-external-events"));
        JsonNode sent = body(request).get("events").get(0);
        Assert.assertEquals(sent.get("name").asText(), "network-changed");
        Assert.assertEquals(sent.get("server_uuid").asText(), SERVER);
        Assert.assertEquals(sent.get("tag").asText(), "port-1");
        Assert.assertFalse(sent.has("status"));
        Assert.assertEquals(events.get(0).getCode(), Integer.valueOf(200));
        Assert.assertEquals(events.get(0).getStatus(), "completed");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.76.*")
    public void powerUpdateEventNeeds276() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.75");
        try {
            osv3().compute().serverExternalEvents().create(Collections.singletonList(ExternalEventCreate.of("power-update", SERVER).tag("POWER_ON")));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void assistedVolumeSnapshots() throws Exception {
        respondWith(200, "{\"snapshot\": {\"id\": \"421752a6-acf6-4b2d-bc7a-119f9148cd8c\", \"volumeId\": \"521752a6-acf6-4b2d-bc7a-119f9148cd8c\"}}");
        respondWith(204);

        AssistedVolumeSnapshot snapshot = osv3().compute().assistedVolumeSnapshots().create("521752a6-acf6-4b2d-bc7a-119f9148cd8c",
                java.util.Map.of("snapshot_id", "421752a6-acf6-4b2d-bc7a-119f9148cd8c", "type", "qcow2", "new_file", "new_file_name"));
        boolean deleted = osv3().compute().assistedVolumeSnapshots().delete("421752a6-acf6-4b2d-bc7a-119f9148cd8c",
                java.util.Map.of("volume_id", "521752a6-acf6-4b2d-bc7a-119f9148cd8c", "type", "qcow2")).isSuccess();

        RecordedRequest create = takeRequest();
        Assert.assertEquals(body(create).get("snapshot").get("volume_id").asText(), "521752a6-acf6-4b2d-bc7a-119f9148cd8c");
        Assert.assertEquals(body(create).get("snapshot").get("create_info").get("type").asText(), "qcow2");
        RecordedRequest delete = takeRequest();
        Assert.assertEquals(delete.getMethod(), "DELETE");
        Assert.assertTrue(delete.getPath().contains("/os-assisted-volume-snapshots/421752a6-acf6-4b2d-bc7a-119f9148cd8c?delete_info="), delete.getPath());
        String deleteInfo = java.net.URLDecoder.decode(delete.getRequestUrl().queryParameter("delete_info"), "UTF-8");
        Assert.assertTrue(deleteInfo.contains("\"type\":\"qcow2\""), deleteInfo);
        Assert.assertEquals(snapshot.getVolumeId(), "521752a6-acf6-4b2d-bc7a-119f9148cd8c");
        Assert.assertTrue(deleted);
    }

    public void consoleAuthToken() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"console\": {\"instance_uuid\": \"" + SERVER + "\", \"host\": \"localhost\", \"port\": 5900, \"tls_port\": 5901, \"internal_access_path\": null}}");

        ConsoleConnectionInfo info = osv3().compute().consoleAuthTokens().get("b60bcfc3-5fd4-4d21-986c-e83379107819");

        Assert.assertTrue(takeRequest().getPath().endsWith("/os-console-auth-tokens/b60bcfc3-5fd4-4d21-986c-e83379107819"));
        Assert.assertEquals(info.getInstanceUuid(), SERVER);
        Assert.assertEquals(info.getPort(), Integer.valueOf(5900));
        Assert.assertEquals(info.getTlsPort(), Integer.valueOf(5901));
        Assert.assertNull(info.getInternalAccessPath());
    }

    public void instanceUsageAuditLogs() throws Exception {
        respondWith(200, "{\"instance_usage_audit_logs\": {\"period_beginning\": \"2026-09-01 00:00:00\", \"period_ending\": \"2026-10-01 00:00:00\","
                + " \"num_hosts\": 1, \"num_hosts_done\": 1, \"num_hosts_running\": 0, \"num_hosts_not_run\": 0, \"hosts_not_run\": [],"
                + " \"total_instances\": 3, \"total_errors\": 0, \"overall_status\": \"ALL hosts done. 0 errors.\", \"log\": {\"compute-1\": {\"state\": \"DONE\"}}}}");
        respondWith(200, "{\"instance_usage_audit_log\": {\"period_beginning\": \"2026-08-01 00:00:00\", \"period_ending\": \"2026-09-01 00:00:00\","
                + " \"num_hosts\": 0, \"hosts_not_run\": [\"compute-2\"], \"total_instances\": 0, \"total_errors\": 0, \"overall_status\": \"x\", \"log\": {}}}");

        InstanceUsageAuditLog current = osv3().compute().instanceUsageAuditLogs().list();
        InstanceUsageAuditLog before = osv3().compute().instanceUsageAuditLogs().get("2026-09-01 00:00:00");

        Assert.assertTrue(takeRequest().getPath().endsWith("/os-instance_usage_audit_log"));
        Assert.assertTrue(takeRequest().getPath().contains("/os-instance_usage_audit_log/2026-09-01"));
        Assert.assertEquals(current.getTotalInstances(), Integer.valueOf(3));
        Assert.assertEquals(current.getOverallStatus(), "ALL hosts done. 0 errors.");
        Assert.assertTrue(current.getLog().containsKey("compute-1"));
        Assert.assertEquals(before.getHostsNotRun(), Arrays.asList("compute-2"));
        Assert.assertEquals(before.getPeriodBeginning(), "2026-08-01 00:00:00");
    }
}
