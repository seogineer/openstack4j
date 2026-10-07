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
}
