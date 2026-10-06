package org.openstack4j.api.image.v2.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.image.v2.ext.ImageStore;
import org.openstack4j.model.image.v2.ext.ImageUsage;
import org.openstack4j.model.image.v2.ext.ImageVersions;
import org.openstack4j.openstack.image.v2.internal.ImageVersionDiscovery;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/Basics")
public class ImageBasicsTests extends AbstractImageExtTest {

    private static final String VERSIONS = "{\"versions\": [{\"id\": \"v2.17\", \"status\": \"CURRENT\", \"links\": []}, {\"id\": \"v2.15\", \"status\": \"SUPPORTED\", \"links\": []},"
            + " {\"id\": \"v2.6\", \"status\": \"SUPPORTED\", \"links\": []}]}";

    public void versionsGoesToRoot() throws Exception {
        respondWith(300, VERSIONS);
        ImageVersions versions = osv3().imagesV2().versions();
        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getPath(), "/versions");
        Assert.assertEquals(versions.getCurrent(), "2.17");
        Assert.assertEquals(versions.getVersions().size(), 3);
        Assert.assertTrue(versions.supports("2.6"));
        Assert.assertTrue(versions.supports("2.17"));
        Assert.assertFalse(versions.supports("2.18"));
        Assert.assertFalse(versions.supports("3.0"));
    }

    public void rootUrlStripsVersionSegment() {
        Assert.assertEquals(ImageVersionDiscovery.rootUrl("http://g:9292"), "http://g:9292");
        Assert.assertEquals(ImageVersionDiscovery.rootUrl("http://g:9292/v2"), "http://g:9292");
        Assert.assertEquals(ImageVersionDiscovery.rootUrl("http://g:9292/v2/"), "http://g:9292");
        Assert.assertEquals(ImageVersionDiscovery.rootUrl("http://g/image/v2.1"), "http://g/image");
    }

    public void importMethods() throws Exception {
        respondWith(200, "{\"import-methods\": {\"description\": \"Import methods available.\", \"type\": \"array\", \"value\": [\"glance-direct\", \"web-download\"]}}");
        List<String> methods = osv3().imagesV2().info().importMethods();
        expect("GET", "/v2/info/import");
        Assert.assertEquals(methods, List.of("glance-direct", "web-download"));
    }

    public void storesReadStringBooleans() throws Exception {
        respondWith(200, "{\"stores\": [{\"id\": \"reliable\", \"description\": \"d\"}, {\"id\": \"fast\", \"description\": \"q\", \"default\": \"true\"},"
                + " {\"id\": \"special\", \"description\": \"s\", \"read-only\": \"true\"}]}");
        respondWith(200, "{\"stores\": [{\"id\": \"reliable\", \"type\": \"rbd\", \"description\": \"d\", \"default\": \"true\", \"weight\": 100,"
                + " \"properties\": {\"pool\": \"pool1\", \"chunk_size\": 65536, \"thin_provisioning\": false}}]}");

        List<? extends ImageStore> stores = osv3().imagesV2().info().stores();
        List<? extends ImageStore> detail = osv3().imagesV2().info().storesDetail();

        expect("GET", "/v2/info/stores");
        expect("GET", "/v2/info/stores/detail");
        Assert.assertNull(stores.get(0).isDefault());
        Assert.assertTrue(stores.get(1).isDefault());
        Assert.assertTrue(stores.get(2).isReadOnly());
        Assert.assertEquals(detail.get(0).getType(), "rbd");
        Assert.assertEquals(detail.get(0).getWeight(), Integer.valueOf(100));
        Assert.assertEquals(detail.get(0).getProperties().get("pool"), "pool1");
    }

    public void usage() throws Exception {
        respondWith(200, "{\"usage\": {\"image_size_total\": {\"limit\": 1024, \"usage\": 256}, \"image_count_total\": {\"limit\": 10, \"usage\": 2}}}");
        Map<String, ? extends ImageUsage> usage = osv3().imagesV2().info().usage();
        expect("GET", "/v2/info/usage");
        Assert.assertEquals(usage.get("image_size_total").getUsage(), Long.valueOf(256));
        Assert.assertEquals(usage.get("image_count_total").getLimit(), Long.valueOf(10));
    }

    public void infoNotFoundIsRaised() throws Exception {
        respondWith(404, "<html><body><h1>404 Not Found</h1></body></html>");
        try {
            osv3().imagesV2().info().usage();
            Assert.fail("expected the 404 to surface");
        } catch (RuntimeException expected) {
            Assert.assertNotNull(expected.getMessage());
        }
        takeRequest();
    }
}
