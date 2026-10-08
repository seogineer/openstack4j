package org.openstack4j.api.ext.small;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "ObjectStorage/Info")
public class SwiftInfoTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.OBJECT_STORAGE;
    }

    public void infoAndEndpoints() throws Exception {
        respondWith(200, "{\"swift\": {\"version\": \"2.33.0\", \"max_file_size\": 5368709122}, \"slo\": {\"max_manifest_segments\": 1000}}");
        respondWith(200, "[\"http://10.1.1.1:6200/sda1/2/AUTH_test/c/o\", \"http://10.1.1.2:6200/sdb1/2/AUTH_test/c/o\"]");

        Map<String, Object> info = osv3().objectStorage().info();
        List<String> endpoints = osv3().objectStorage().listEndpoints("c", "o");

        String infoPath = URLDecoder.decode(takeRequest().getPath(), StandardCharsets.UTF_8);
        String endpointsPath = URLDecoder.decode(takeRequest().getPath(), StandardCharsets.UTF_8);
        Assert.assertEquals(infoPath, "/info");
        Assert.assertTrue(endpointsPath.startsWith("/endpoints/") && endpointsPath.endsWith("/c/o"), endpointsPath);
        Assert.assertFalse(endpointsPath.contains("/v1/"), endpointsPath);
        Assert.assertEquals(((Map<?, ?>) info.get("swift")).get("version"), "2.33.0");
        Assert.assertEquals(endpoints.size(), 2);
    }
}
