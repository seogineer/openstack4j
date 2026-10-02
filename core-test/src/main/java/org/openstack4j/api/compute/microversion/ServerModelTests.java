package org.openstack4j.api.compute.microversion;

import java.util.Arrays;

import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServerModel")
public class ServerModelTests extends AbstractComputeMicroVersionTest {

    public void readsMicroVersionFieldsOn2100() throws Exception {
        negotiate("2.100");
        respondWith("/compute/microversion/server_2_100.json");

        Server server = osv3().compute().servers().get(SERVER);
        takeRequest();

        Assert.assertEquals(server.getHostname(), "vm-1");
        Assert.assertEquals(server.getReservationId(), "r-1ewf37jz");
        Assert.assertEquals(server.getLaunchIndex(), Integer.valueOf(0));
        Assert.assertEquals(server.getRootDeviceName(), "/dev/vda");
        Assert.assertEquals(server.getLocked(), Boolean.TRUE);
        Assert.assertEquals(server.getLockedReason(), "maintenance");
        Assert.assertEquals(server.getHostStatus(), "UP");
        Assert.assertEquals(server.getDescription(), "web server");
        Assert.assertEquals(server.getTags(), Arrays.asList("web", "prod"));
        Assert.assertNull(server.getTrustedImageCertificates());
        Assert.assertEquals(server.getServerGroups(), Arrays.asList("4c3e2a2e-0000-4000-8000-000000000001"));
        Assert.assertEquals(server.getPinnedAvailabilityZone(), "nova");
        Assert.assertEquals(server.getSchedulerHints().get("group"), "4c3e2a2e-0000-4000-8000-000000000001");
        Assert.assertEquals(server.getImageProperties().get("os_type"), "linux");
        Assert.assertEquals(server.getImageId(), "777afa04-f596-42b1-86ac-7e5881b7d46c");
    }

    public void flavorEmbeddedFormDoesNotTriggerLookup() throws Exception {
        negotiate("2.100");
        respondWith("/compute/microversion/server_2_100.json");

        Server server = osv3().compute().servers().get(SERVER);
        takeRequest();
        Flavor summary = server.getFlavorSummary();
        Flavor flavor = server.getFlavor();

        Assert.assertNull(server.getFlavorId());
        Assert.assertSame(flavor, summary);
        Assert.assertEquals(summary.getOriginalName(), "m1.tiny");
        Assert.assertEquals(summary.getVcpus(), 1);
        Assert.assertEquals(summary.getRam(), 1024);
        Assert.assertEquals(summary.getEphemeral(), 2);
        Assert.assertEquals(summary.getExtraSpecs().get("test1"), "test");
        assertNoMoreRequests();
    }

    public void flavorReferenceFormStillLooksUpOn21() throws Exception {
        respondWith("/compute/microversion/server_2_1.json");
        respondWith(200, "{\"flavor\": {\"id\": \"m1.tiny\", \"name\": \"m1.tiny\", \"ram\": 512, \"vcpus\": 1, \"disk\": 1}}");

        Server server = osv3().compute().servers().get(SERVER);
        takeRequest();

        Assert.assertEquals(server.getFlavorId(), "m1.tiny");
        Assert.assertNull(server.getFlavorSummary().getName());
        Assert.assertEquals(server.getFlavor().getName(), "m1.tiny");
        Assert.assertTrue(takeRequest().getPath().endsWith("/flavors/m1.tiny"));
        Assert.assertNull(server.getHostname());
        Assert.assertNull(server.getTags());
        Assert.assertNull(server.getImageProperties());
    }

    public void volumeAttachmentReads289Fields() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"volumeAttachment\": {\"device\": \"/dev/vdb\", \"serverId\": \"" + SERVER + "\", \"volumeId\": \"v1\","
                + " \"attachment_id\": \"a1\", \"bdm_uuid\": \"b1\", \"tag\": \"data\", \"delete_on_termination\": true}}");

        var attachment = osv3().compute().servers().attachVolume(SERVER, "v1", "/dev/vdb");

        Assert.assertNull(attachment.getId());
        Assert.assertEquals(attachment.getAttachmentId(), "a1");
        Assert.assertEquals(attachment.getBdmUuid(), "b1");
        Assert.assertEquals(attachment.getTag(), "data");
        Assert.assertEquals(attachment.getDeleteOnTermination(), Boolean.TRUE);
    }
}
