package org.openstack4j.api.compute.microversion;

import java.util.Collections;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.VNCConsole;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/LegacyCeilings")
public class LegacyCeilingTests extends AbstractComputeMicroVersionTest {

    public void legacyMethodsAreCappedForProxies() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"floating_ips\": []}");
        respondWith(200, "{\"security_groups\": []}");
        respondWith(200, "{\"images\": []}");

        osv3().compute().floatingIps().list();
        osv3().compute().securityGroups().list();
        osv3().compute().images().list();

        assertVersionHeaders(takeRequest(), "2.35");
        assertVersionHeaders(takeRequest(), "2.35");
        assertVersionHeaders(takeRequest(), "2.35");
    }

    public void legacyMethodsAreCappedForHostsAndConsolesAndDiagnostics() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"hosts\": []}");
        respondWith(200, "{\"console\": {\"type\": \"novnc\", \"url\": \"http://x\"}}");
        respondWith(200, "{\"cpu0_time\": 1}");

        osv3().compute().host().list();
        osv3().compute().servers().getVNCConsole(SERVER, VNCConsole.Type.NOVNC);
        osv3().compute().servers().diagnostics(SERVER);

        assertVersionHeaders(takeRequest(), "2.42");
        assertVersionHeaders(takeRequest(), "2.5");
        assertVersionHeaders(takeRequest(), "2.47");
    }

    public void legacyKeypairWithoutPublicKeyIsCapped() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"keypair\": {\"name\": \"k\", \"public_key\": \"ssh-rsa A\", \"private_key\": \"P\", \"fingerprint\": \"f\"}}");
        respondWith(200, "{\"keypair\": {\"name\": \"k2\", \"public_key\": \"ssh-rsa B\", \"fingerprint\": \"f\"}}");

        osv3().compute().keypairs().create("k", null);
        osv3().compute().keypairs().create("k2", "ssh-rsa B");

        assertVersionHeaders(takeRequest(), "2.91");
        assertVersionHeaders(takeRequest(), "2.100");
    }

    public void legacyServicesEnableIsCappedAndForceDownKeepsLegacyHeaderWhenDisabled() throws Exception {
        respondWith(200, "{\"service\": {\"binary\": \"nova-compute\", \"host\": \"h\", \"forced_down\": true}}");
        osv3().compute().services().forceDownService("nova-compute", "h");
        RecordedRequest legacy = takeRequest();
        Assert.assertEquals(legacy.getHeader("x-openstack-nova-api-version"), "2.11");
        Assert.assertNull(legacy.getHeader("OpenStack-API-Version"));

        negotiate("2.100");
        respondWith(200, "{\"service\": {\"binary\": \"nova-compute\", \"host\": \"h\", \"status\": \"enabled\"}}");
        respondWith(200, "{\"service\": {\"binary\": \"nova-compute\", \"host\": \"h\", \"forced_down\": true}}");
        osv3().compute().services().enableService("nova-compute", "h");
        osv3().compute().services().forceDownService("nova-compute", "h");
        assertVersionHeaders(takeRequest(), "2.52");
        assertVersionHeaders(takeRequest(), "2.52");
    }

    public void legacyHypervisorStatisticsAndServerGroupCreateAreCapped() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"hypervisor_statistics\": {\"count\": 1}}");
        respondWith(200, "{\"server_group\": {\"id\": \"g\", \"name\": \"n\", \"policies\": [\"affinity\"]}}");

        osv3().compute().hypervisors().statistics();
        osv3().compute().serverGroups().create("n", "affinity");

        assertVersionHeaders(takeRequest(), "2.87");
        assertVersionHeaders(takeRequest(), "2.63");
    }

    public void bootWithoutNetworksOrWithPersonalityIsCapped() throws Exception {
        negotiate("2.100");
        respondWith(202, "{\"server\": {\"id\": \"s1\"}}");
        respondWith(202, "{\"server\": {\"id\": \"s2\"}}");
        respondWith(202, "{\"server\": {\"id\": \"s3\"}}");

        ServerCreate noNetworks = Builders.server().name("a").flavor("f").image("i").build();
        ServerCreate withPersonality = Builders.server().name("b").flavor("f").image("i").networks(Collections.singletonList("n1"))
                .addPersonality("/etc/motd", "hello").build();
        ServerCreate modern = Builders.server().name("c").flavor("f").image("i").networks(Collections.singletonList("n1")).build();
        osv3().compute().servers().boot(noNetworks);
        osv3().compute().servers().boot(withPersonality);
        osv3().compute().servers().boot(modern);

        assertVersionHeaders(takeRequest(), "2.36");
        assertVersionHeaders(takeRequest(), "2.56");
        assertVersionHeaders(takeRequest(), "2.100");
    }

    public void legacyAttachVolumeAndFlavorCreateAreCapped() throws Exception {
        negotiate("2.104");
        respondWith(200, "{\"volumeAttachment\": {\"device\": \"/dev/vdb\", \"id\": \"v1\", \"serverId\": \"s\", \"volumeId\": \"v1\"}}");
        respondWith(200, "{\"flavor\": {\"id\": \"f1\", \"name\": \"tiny\"}}");

        osv3().compute().servers().attachVolume(SERVER, "v1", "/dev/vdb");
        osv3().compute().flavors().create("tiny", 512, 1, 1, 0, 0, 1.0f, true);

        assertVersionHeaders(takeRequest(), "2.100");
        assertVersionHeaders(takeRequest(), "2.101");
    }

    public void createSnapshotReadsImageIdFromBodyOn245() throws Exception {
        negotiate("2.100");
        respondWith(202, "{\"image_id\": \"0e7761dd-ee98-41f0-ba35-05994e446431\"}");

        String imageId = osv3().compute().servers().createSnapshot(SERVER, "snap");

        assertVersionHeaders(takeRequest(), "2.100");
        Assert.assertEquals(imageId, "0e7761dd-ee98-41f0-ba35-05994e446431");
    }

    public void createSnapshotReadsLocationWhenDisabled() throws Exception {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Location", "http://glance/v2/images/abc-123");
        respondWith(headers, 202);

        Assert.assertEquals(osv3().compute().servers().createSnapshot(SERVER, "snap"), "abc-123");
        assertNoVersionHeaders(takeRequest());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.26.*")
    public void serverTagsRequire226WhenEnabled() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.20");
        try {
            osv3().compute().serverTags().list(SERVER);
        } finally {
            assertNoMoreRequests();
        }
    }
}
