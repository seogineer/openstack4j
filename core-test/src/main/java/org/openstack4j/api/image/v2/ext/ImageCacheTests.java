package org.openstack4j.api.image.v2.ext;

import org.openstack4j.model.image.v2.ext.ImageCacheState;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Image/V2/Ext/Cache")
public class ImageCacheTests extends AbstractImageExtTest {

    public void cacheLifecycle() throws Exception {
        respondWith(200, "{\"cached_images\": [{\"image_id\": \"fe05d6c9\", \"hits\": 0, \"last_accessed\": 1651504844.0860524, \"last_modified\": 1651504844.0860524, \"size\": 987654}],"
                + " \"queued_images\": [\"e34e6e2f\", \"6b9fbf2b\"]}");
        for (int i = 0; i < 6; i++)
            respondWith(i < 2 ? 202 : 204);

        var cache = osv3().imagesV2().cache();
        ImageCacheState state = cache.list();
        Assert.assertTrue(cache.queue("e34e6e2f").isSuccess());
        Assert.assertTrue(cache.clean().isSuccess());
        cache.prune();
        cache.delete("fe05d6c9");
        cache.clear();
        cache.clear("queue");

        expect("GET", "/v2/cache");
        expect("PUT", "/v2/cache/e34e6e2f");
        expect("POST", "/v2/cache/clean");
        expect("POST", "/v2/cache/prune");
        expect("DELETE", "/v2/cache/fe05d6c9");
        Assert.assertNull(expect("DELETE", "/v2/cache").getHeader("x-image-cache-clear-target"));
        Assert.assertEquals(expect("DELETE", "/v2/cache").getHeader("x-image-cache-clear-target"), "queue");
        Assert.assertEquals(state.getCachedImages().get(0).getSize(), Long.valueOf(987654));
        Assert.assertEquals(state.getCachedImages().get(0).getLastAccessed(), 1651504844.0860524, 1e-6);
        Assert.assertEquals(state.getQueuedImages().size(), 2);
    }

    public void cacheDisabledIsRaised() throws Exception {
        respondWith(java.util.Collections.singletonMap("Content-Type", "text/html; charset=UTF-8"), 404, "<html><body><h1>404 Not Found</h1>The resource could not be found.</body></html>");
        try {
            osv3().imagesV2().cache().list();
            Assert.fail("expected the 404 to surface");
        } catch (RuntimeException expected) {
            Assert.assertFalse(String.valueOf(expected.getMessage()).contains("Unexpected character"), "parse error instead of the 404: " + expected.getMessage());
        }
        takeRequest();
    }
}
