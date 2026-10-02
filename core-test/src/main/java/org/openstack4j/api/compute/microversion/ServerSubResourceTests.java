package org.openstack4j.api.compute.microversion;

import java.util.Arrays;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.RemoteConsole;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerDiagnosticsStandard;
import org.openstack4j.model.compute.ServerTopology;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

@Test(suiteName = "Compute/ServerSubResources")
public class ServerSubResourceTests extends AbstractComputeMicroVersionTest {

    public void topology() throws Exception {
        negotiate("2.100");
        respondWith("/compute/microversion/topology.json");

        ServerTopology topology = osv3().compute().servers().topology(SERVER);

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/servers/" + SERVER + "/topology"));
        assertVersionHeaders(request, "2.100");
        Assert.assertEquals(topology.getPagesizeKb(), Integer.valueOf(4));
        Assert.assertEquals(topology.getNodes().size(), 2);
        Assert.assertEquals(topology.getNodes().get(1).getMemoryMb(), Integer.valueOf(2048));
        Assert.assertEquals(topology.getNodes().get(0).getCpuPinning().get("1"), Integer.valueOf(5));
        Assert.assertEquals(topology.getNodes().get(0).getSiblings().get(0), Arrays.asList(0, 1));
        Assert.assertEquals(topology.getNodes().get(1).getVcpuSet(), Arrays.asList(2, 3));
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.78.*")
    public void topologyNeeds278() throws Exception {
        try {
            osv3().compute().servers().topology(SERVER);
        } finally {
            assertNoMoreRequests();
        }
    }

    public void ipsAllAndByNetwork() throws Exception {
        respondWith(200, "{\"addresses\": {\"private\": [{\"version\": 4, \"addr\": \"10.0.0.5\"}], \"public\": [{\"version\": 6, \"addr\": \"2001:db8::5\"}]}}");
        respondWith(200, "{\"private\": [{\"version\": 4, \"addr\": \"10.0.0.5\"}]}");

        Addresses all = osv3().compute().servers().ips(SERVER);
        List<? extends Address> priv = osv3().compute().servers().ips(SERVER, "private");

        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/ips"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/servers/" + SERVER + "/ips/private"));
        Assert.assertEquals(all.getAddresses("public").get(0).getAddr(), "2001:db8::5");
        Assert.assertEquals(priv.size(), 1);
        Assert.assertEquals(priv.get(0).getAddr(), "10.0.0.5");
    }

    public void remoteConsole() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"remote_console\": {\"protocol\": \"vnc\", \"type\": \"novnc\", \"url\": \"http://10.0.0.1:6080/vnc_auto.html?token=t\"}}");

        RemoteConsole console = osv3().compute().servers().remoteConsole(SERVER, "vnc", "novnc");

        RecordedRequest request = takeRequest();
        Assert.assertTrue(request.getPath().endsWith("/servers/" + SERVER + "/remote-consoles"));
        Assert.assertEquals(body(request).get("remote_console").get("protocol").asText(), "vnc");
        Assert.assertEquals(body(request).get("remote_console").get("type").asText(), "novnc");
        Assert.assertEquals(console.getUrl(), "http://10.0.0.1:6080/vnc_auto.html?token=t");
        Assert.assertEquals(console.getType(), "novnc");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.99.*")
    public void spiceDirectNeeds299() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.98");
        try {
            osv3().compute().servers().remoteConsole(SERVER, "spice", "spice-direct");
        } finally {
            assertNoMoreRequests();
        }
    }

    public void diagnosticsStandard() throws Exception {
        negotiate("2.100");
        respondWith("/compute/microversion/diagnostics_2_48.json");

        ServerDiagnosticsStandard d = osv3().compute().servers().diagnosticsStandard(SERVER);

        assertVersionHeaders(takeRequest(), "2.100");
        Assert.assertEquals(d.getDriver(), "libvirt");
        Assert.assertEquals(d.getUptime(), Long.valueOf(858833));
        Assert.assertEquals(d.getConfigDrive(), Boolean.FALSE);
        Assert.assertEquals(d.getCpuDetails().get(0).getTime(), Long.valueOf(10897320000000L));
        Assert.assertNull(d.getCpuDetails().get(0).getUtilisation());
        Assert.assertEquals(d.getDiskDetails().get(0).getErrorsCount(), Long.valueOf(-1));
        Assert.assertEquals(d.getNicDetails().get(0).getMacAddress(), "fa:16:3e:ee:08:6f");
        Assert.assertEquals(d.getMemoryDetails().getMaximum(), Long.valueOf(1048576));
    }

    public void attachVolumeAsync() throws Exception {
        negotiate("2.104");
        respondWith(202);

        Assert.assertTrue(osv3().compute().servers().attachVolumeAsync(SERVER, "v1", null).isSuccess());

        RecordedRequest request = takeRequest();
        assertVersionHeaders(request, "2.104");
        Assert.assertTrue(request.getPath().endsWith("/servers/" + SERVER + "/os-volume_attachments"));
        Assert.assertEquals(body(request).get("volumeAttachment").get("volumeId").asText(), "v1");
        Assert.assertFalse(body(request).get("volumeAttachment").has("device"));
    }

    public void unpinAvailabilityZoneSendsNull() throws Exception {
        negotiate("2.104");
        respondWith("/compute/microversion/server_2_100.json");

        Server server = osv3().compute().servers().updatePinnedAvailabilityZone(SERVER, null);

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertTrue(body(request).get("server").has("pinned_availability_zone"));
        Assert.assertTrue(body(request).get("server").get("pinned_availability_zone").isNull());
        Assert.assertEquals(server.getId(), SERVER);
    }
}
