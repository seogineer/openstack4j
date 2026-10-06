package org.openstack4j.api.image.v2.ext;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.image.v2.Task;
import org.openstack4j.model.image.v2.ext.ImageLocation;
import org.openstack4j.model.image.v2.options.ImageImportOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/Import")
public class ImageImportTests extends AbstractImageExtTest {

    private static final String IMAGE = "fe05d6c9-ef02-4161-9056-81ed046f3024";

    public void importMethodsBodies() throws Exception {
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(202);

        var images = osv3().imagesV2();
        Assert.assertTrue(images.importImage(IMAGE, ImageImportOptions.glanceDirect()).isSuccess());
        images.importImage(IMAGE, ImageImportOptions.webDownload("https://example.com/cirros.img").allStores(true).allStoresMustSucceed(true));
        images.importImage(IMAGE, ImageImportOptions.copyImage(List.of("fast", "cheap")).allStoresMustSucceed(false));
        images.importImage(IMAGE, ImageImportOptions.glanceDownload("RegionTwo", "remote1").serviceInterface("public"));

        JsonNode direct = body(expect("POST", "/v2/images/" + IMAGE + "/import"));
        Assert.assertEquals(direct.toString(), "{\"method\":{\"name\":\"glance-direct\"}}");
        JsonNode web = body(expect("POST", "/v2/images/" + IMAGE + "/import"));
        Assert.assertEquals(web.get("method").get("uri").asText(), "https://example.com/cirros.img");
        Assert.assertTrue(web.get("all_stores").asBoolean());
        JsonNode copy = body(expect("POST", "/v2/images/" + IMAGE + "/import"));
        Assert.assertEquals(copy.get("method").get("name").asText(), "copy-image");
        Assert.assertEquals(copy.get("stores").get(1).asText(), "cheap");
        Assert.assertFalse(copy.get("all_stores_must_succeed").asBoolean());
        JsonNode download = body(expect("POST", "/v2/images/" + IMAGE + "/import"));
        Assert.assertEquals(download.get("method").get("glance_region").asText(), "RegionTwo");
        Assert.assertEquals(download.get("method").get("glance_image_id").asText(), "remote1");
        Assert.assertEquals(download.get("method").get("glance_service_interface").asText(), "public");
    }

    public void stageSendsOctetStream() throws Exception {
        respondWith(204);
        byte[] data = "image-bytes".getBytes(StandardCharsets.UTF_8);
        Assert.assertTrue(osv3().imagesV2().stage(IMAGE, Payloads.create(new ByteArrayInputStream(data))).isSuccess());
        RecordedRequest request = expect("PUT", "/v2/images/" + IMAGE + "/stage");
        Assert.assertTrue(request.getHeader("Content-Type").startsWith("application/octet-stream"), request.getHeader("Content-Type"));
        Assert.assertEquals(request.getBody().readUtf8(), "image-bytes");
    }

    public void locationsReadRootArray() throws Exception {
        respondWith(200, "[{\"url\": \"cinder://lvmdriver-1/39e6ffab\", \"metadata\": {\"store\": \"lvmdriver-1\"}}]");
        respondWith(202);

        List<? extends ImageLocation> locations = osv3().imagesV2().listLocations(IMAGE);
        boolean added = osv3().imagesV2().addLocation(IMAGE, "cinder://lvmdriver-1/39e6ffab", Map.of("os_hash_algo", "sha512", "os_hash_value", "c504")).isSuccess();

        expect("GET", "/v2/images/" + IMAGE + "/locations");
        RecordedRequest add = expect("POST", "/v2/images/" + IMAGE + "/locations");
        Assert.assertEquals(body(add).get("url").asText(), "cinder://lvmdriver-1/39e6ffab");
        Assert.assertEquals(body(add).get("validation_data").get("os_hash_algo").asText(), "sha512");
        Assert.assertEquals(locations.get(0).getMetadata().get("store"), "lvmdriver-1");
        Assert.assertTrue(added);
    }

    public void addLocationWithoutValidationDataOmitsIt() throws Exception {
        respondWith(202);
        osv3().imagesV2().addLocation(IMAGE, "file:///var/lib/glance/x", null);
        Assert.assertFalse(body(takeRequest()).has("validation_data"));
    }

    public void imageTasksAndStoreDelete() throws Exception {
        respondWith(200, "{\"tasks\": [{\"id\": \"ee22890e\", \"image_id\": \"" + IMAGE + "\", \"request-id\": \"r1\", \"user\": \"u1\", \"type\": \"api_image_import\","
                + " \"status\": \"processing\", \"owner\": \"o1\", \"expires_at\": null, \"created_at\": \"2020-12-18T05:20:38.000000\", \"updated_at\": \"2020-12-18T05:25:39.000000\", \"deleted\": false, \"message\": \"\"}]}");
        respondWith(204);

        List<? extends Task> tasks = osv3().imagesV2().listTasks(IMAGE);
        boolean deleted = osv3().imagesV2().deleteFromStore("fast", IMAGE).isSuccess();

        expect("GET", "/v2/images/" + IMAGE + "/tasks");
        expect("DELETE", "/v2/stores/fast/" + IMAGE);
        Assert.assertEquals(tasks.get(0).getId(), "ee22890e");
        Assert.assertTrue(deleted);
    }
}
