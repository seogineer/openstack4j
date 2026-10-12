package org.openstack4j.api.image.v2.ext;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Deferred minor from the image review: version checks reject null clearly. */
@Test(suiteName = "Image/V2/Ext/QualityFixes")
public class ImageQualityFixTests extends AbstractImageExtTest {

    public void supportsNullFailsClearly() throws Exception {
        respondWith(300, "{\"versions\": [{\"id\": \"v2.17\", \"status\": \"CURRENT\"}]}");
        var versions = osv3().imagesV2().versions();
        takeRequest();
        try {
            versions.supports(null);
            Assert.fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            Assert.assertEquals(expected.getMessage(), "version");
        }
    }

    public void nonNumericVersionPartsCompareByTheirDigits() throws Exception {
        respondWith(300, "{\"versions\": [{\"id\": \"v2.17-beta\", \"status\": \"CURRENT\"}]}");
        var versions = osv3().imagesV2().versions();
        takeRequest();
        Assert.assertTrue(versions.supports("2.6"));
        Assert.assertTrue(versions.supports("2.17"));
        Assert.assertFalse(versions.supports("2.18"));
        respondWith(300, "{\"versions\": [{\"id\": \"v2.x\", \"status\": \"SUPPORTED\"}, {\"id\": \"v2.9\", \"status\": \"SUPPORTED\"}]}");
        Assert.assertEquals(osv3().imagesV2().versions().getCurrent(), "2.9");
        takeRequest();
    }
}
