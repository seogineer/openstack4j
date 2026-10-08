package org.openstack4j.api.accelerator;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Accelerator")
public class AcceleratorTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.ACCELERATOR;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    private static String json(RecordedRequest r) throws Exception {
        return new ObjectMapper().readTree(r.getBody().readUtf8()).toString();
    }

    public void arqsAndProfiles() throws Exception {
        respondWith(201, "{\"arqs\": [{\"uuid\": \"a1\", \"state\": \"Initial\", \"device_profile_name\": \"dp1\"}]}");
        respondWith(200, "{\"arqs\": [{\"uuid\": \"a1\"}]}");
        respondWith(200, "{\"uuid\": \"a1\", \"state\": \"Bound\"}");
        respondWith(202);
        respondWith(204);
        respondWith(204);
        respondWith(201, "{\"name\": \"dp1\", \"uuid\": \"p1\", \"groups\": [{\"resources:CUSTOM_ACCELERATOR_FPGA\": \"1\"}]}");
        respondWith(200, "{\"device_profile\": {\"name\": \"dp1\", \"uuid\": \"p1\"}}");
        respondWith(200, "{\"device_profile\": {\"name\": \"dp1\", \"uuid\": \"1a939c88-0b01-408b-bab0-4c61d3a02d71\"}}");
        respondWith(204);

        var acc = osv3().accelerator();
        List<Map<String, Object>> created = acc.createArqs("dp1");
        acc.listArqs(Map.of("instance", "i1"));
        Map<String, Object> arq = acc.getArq("a1");
        Assert.assertTrue(acc.patchArqs(Map.of("a1", List.of(Map.of("op", "add", "path", "/project_id", "value", "p9")))).isSuccess());
        Assert.assertTrue(acc.deleteArqs(List.of("a1", "a2")).isSuccess());
        Assert.assertTrue(acc.deleteArqsOfInstance("i1").isSuccess());
        Map<String, Object> profile = acc.createDeviceProfile(Map.of("name", "dp1", "groups", List.of(Map.of("resources:CUSTOM_ACCELERATOR_FPGA", "1"))));
        Map<String, Object> byName = acc.getDeviceProfile("dp1");
        acc.getDeviceProfile("1a939c88-0b01-408b-bab0-4c61d3a02d71");
        Assert.assertTrue(acc.deleteDeviceProfilesByName(List.of("dp1", "dp2")).isSuccess());

        RecordedRequest create = takeRequest();
        Assert.assertEquals(path(create), "/v2/accelerator_requests");
        Assert.assertEquals(json(create), "{\"device_profile_name\":\"dp1\"}");
        Assert.assertEquals(path(takeRequest()), "/v2/accelerator_requests?instance=i1");
        Assert.assertEquals(path(takeRequest()), "/v2/accelerator_requests/a1");
        RecordedRequest patch = takeRequest();
        Assert.assertEquals(patch.getMethod(), "PATCH");
        Assert.assertEquals(patch.getHeader("OpenStack-API-Version"), "accelerator 2.1");
        Assert.assertEquals(new ObjectMapper().readTree(patch.getBody().readUtf8()).get("a1").get(0).get("path").asText(), "/project_id");
        Assert.assertEquals(path(takeRequest()), "/v2/accelerator_requests?arqs=a1,a2");
        Assert.assertEquals(path(takeRequest()), "/v2/accelerator_requests?instance=i1");
        RecordedRequest profileCreate = takeRequest();
        Assert.assertTrue(json(profileCreate).startsWith("[{"), json(profileCreate));
        Assert.assertEquals(takeRequest().getHeader("OpenStack-API-Version"), "accelerator 2.2");
        Assert.assertNull(takeRequest().getHeader("OpenStack-API-Version"));
        Assert.assertEquals(path(takeRequest()), "/v2/device_profiles?value=dp1,dp2");
        Assert.assertEquals(created.get(0).get("state"), "Initial");
        Assert.assertEquals(arq.get("state"), "Bound");
        Assert.assertEquals(profile.get("uuid"), "p1");
        Assert.assertEquals(byName.get("uuid"), "p1");
    }

    public void devicesDeployablesAttributes() throws Exception {
        respondWith(200, "{\"devices\": [{\"uuid\": \"d1\", \"type\": \"FPGA\", \"status\": \"enabled\"}]}");
        respondWith(200, "{\"uuid\": \"d1\", \"type\": \"FPGA\"}");
        respondWith(204);
        respondWith(204);
        respondWith(200, "{\"deployables\": [{\"uuid\": \"dep1\", \"num_accelerators\": 1}]}");
        respondWith(200, "{\"uuid\": \"dep1\", \"name\": \"fpga0\"}");
        respondWith(200, "{\"uuid\": \"dep1\", \"name\": \"fpga0\"}");
        respondWith(201, "{\"uuid\": \"at1\", \"key\": \"rc\", \"value\": \"FPGA\"}");
        respondWith(204);

        var acc = osv3().accelerator();
        List<Map<String, Object>> devices = acc.listDevices(Map.of("type", "FPGA"));
        acc.getDevice("d1");
        Assert.assertTrue(acc.disableDevice("d1").isSuccess());
        Assert.assertTrue(acc.enableDevice("d1").isSuccess());
        acc.listDeployables(null);
        acc.getDeployable("dep1");
        acc.programDeployable("dep1", "img1");
        Map<String, Object> attr = acc.createAttribute("1", "rc", "FPGA");
        Assert.assertTrue(acc.deleteAttribute("at1").isSuccess());

        RecordedRequest list = takeRequest();
        Assert.assertEquals(path(list), "/v2/devices?type=FPGA");
        Assert.assertNull(list.getHeader("OpenStack-API-Version"));
        Assert.assertEquals(path(takeRequest()), "/v2/devices/d1");
        Assert.assertEquals(path(takeRequest()), "/v2/devices/d1/disable");
        Assert.assertEquals(path(takeRequest()), "/v2/devices/d1/enable");
        Assert.assertEquals(path(takeRequest()), "/v2/deployables");
        Assert.assertEquals(path(takeRequest()), "/v2/deployables/dep1");
        RecordedRequest program = takeRequest();
        Assert.assertEquals(program.getMethod(), "PATCH");
        Assert.assertEquals(json(program), "[{\"path\":\"/program\",\"value\":[{\"image_uuid\":\"img1\"}],\"op\":\"replace\"}]");
        Assert.assertEquals(json(takeRequest()), "{\"deployable_id\":\"1\",\"key\":\"rc\",\"value\":\"FPGA\"}");
        Assert.assertEquals(path(takeRequest()), "/v2/attributes/at1");
        Assert.assertEquals(devices.get(0).get("status"), "enabled");
        Assert.assertEquals(attr.get("key"), "rc");
    }
}
