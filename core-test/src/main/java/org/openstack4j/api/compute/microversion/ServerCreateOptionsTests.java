package org.openstack4j.api.compute.microversion;

import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.ServerListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServerCreateOptions")
public class ServerCreateOptionsTests extends AbstractComputeMicroVersionTest {

    private static final String CREATED = "{\"server\": {\"id\": \"s1\"}}";

    public void newCreateOptionsAreSerialized() throws Exception {
        negotiate("2.100");
        respondWith(202, CREATED);

        ServerCreate create = Builders.server().name("a").flavor("f").image("i")
                .addTaggedNetwork("n1", "nic0")
                .tags(Arrays.asList("web", "prod"))
                .trustedImageCertificates(Collections.singletonList("cert-1"))
                .hostname("web-01")
                .description("frontend")
                .blockDevice(Builders.blockDeviceMapping().uuid("v1").bootIndex(0).tag("root").volumeType("ssd").build())
                .build();
        osv3().compute().servers().boot(create);

        RecordedRequest request = takeRequest();
        assertVersionHeaders(request, "2.100");
        JsonNode server = body(request).get("server");
        Assert.assertEquals(server.get("tags").get(1).asText(), "prod");
        Assert.assertEquals(server.get("trusted_image_certificates").get(0).asText(), "cert-1");
        Assert.assertEquals(server.get("hostname").asText(), "web-01");
        Assert.assertEquals(server.get("description").asText(), "frontend");
        Assert.assertEquals(server.get("networks").get(0).get("uuid").asText(), "n1");
        Assert.assertEquals(server.get("networks").get(0).get("tag").asText(), "nic0");
        Assert.assertEquals(server.get("block_device_mapping_v2").get(0).get("tag").asText(), "root");
        Assert.assertEquals(server.get("block_device_mapping_v2").get(0).get("volume_type").asText(), "ssd");
    }

    public void autoAndNoneNetworksAreStringsAndNotCapped() throws Exception {
        negotiate("2.100");
        respondWith(202, CREATED);
        respondWith(202, CREATED);

        osv3().compute().servers().boot(Builders.server().name("a").flavor("f").image("i").autoAllocateNetwork().build());
        osv3().compute().servers().boot(Builders.server().name("b").flavor("f").image("i").noNetwork().build());

        RecordedRequest auto = takeRequest();
        assertVersionHeaders(auto, "2.100");
        Assert.assertEquals(body(auto).get("server").get("networks").asText(), "auto");
        Assert.assertEquals(body(takeRequest()).get("server").get("networks").asText(), "none");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.90.*")
    public void hostnameNeeds290() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.89");
        try {
            osv3().compute().servers().boot(Builders.server().name("a").flavor("f").image("i").networks(Collections.singletonList("n1")).hostname("x").build());
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class)
    public void newOptionWhileDisabledIsRejected() throws Exception {
        try {
            osv3().compute().servers().boot(Builders.server().name("a").flavor("f").image("i").tags(Collections.singletonList("t")).build());
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*combined.*")
    public void personalityWithHostnameIsRejected() throws Exception {
        negotiate("2.100");
        try {
            osv3().compute().servers().boot(Builders.server().name("a").flavor("f").image("i").networks(Collections.singletonList("n1"))
                    .addPersonality("/etc/motd", "hi").hostname("x").build());
        } finally {
            assertNoMoreRequests();
        }
    }

    public void legacyHostOptionUncheckedWhenDisabled() throws Exception {
        respondWith(202, CREATED);
        osv3().compute().servers().boot(Builders.server().name("a").flavor("f").image("i").host("compute-1").build());
        assertNoVersionHeaders(takeRequest());
    }

    public void listOptionsBecomeQueryAndCheckFloor() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"servers\": []}");

        osv3().compute().servers().list(ServerListOptions.create().locked(true).tags("web", "prod").changesBefore("2026-10-01T00:00:00Z").limit(5).availabilityZone("nova"));

        RecordedRequest request = takeRequest();
        assertVersionHeaders(request, "2.100");
        String path = request.getPath();
        Assert.assertTrue(path.contains("/servers/detail?"), path);
        Assert.assertTrue(path.contains("locked=true"), path);
        Assert.assertTrue(path.contains("tags=web%2Cprod"), path);
        Assert.assertTrue(path.contains("changes-before=2026-10-01T00%3A00%3A00Z"), path);
        Assert.assertTrue(path.contains("limit=5"), path);
        Assert.assertTrue(path.contains("availability_zone=nova"), path);
        Assert.assertEquals(ServerListOptions.create().locked(true).getRequiredMicroVersion(), "2.73");
        Assert.assertNull(ServerListOptions.create().name("x").getRequiredMicroVersion());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.66.*")
    public void listFloorIsChecked() throws Exception {
        negotiate("2.100");
        osv3().compute().microVersions().use("2.60");
        try {
            osv3().compute().servers().list(ServerListOptions.create().changesBefore("2026-10-01T00:00:00Z"));
        } finally {
            assertNoMoreRequests();
        }
    }
}
