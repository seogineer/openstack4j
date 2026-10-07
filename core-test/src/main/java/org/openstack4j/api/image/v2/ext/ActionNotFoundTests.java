package org.openstack4j.api.image.v2.ext;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.v2.options.ImageImportOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Methods returning an ActionResponse report a 404 as a failed response (like upload and deletes), never by throwing. */
@Test(suiteName = "Image/V2/Ext/ActionNotFound")
public class ActionNotFoundTests extends AbstractImageExtTest {

    private void assertFailed(java.util.function.Supplier<ActionResponse> call) throws Exception {
        respondWith(java.util.Collections.singletonMap("Content-Type", "text/html; charset=UTF-8"), 404, "<html><body><h1>404 Not Found</h1>The resource could not be found.</body></html>");
        ActionResponse response = call.get();
        takeRequest();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getCode(), 404);
    }

    public void importOnOldServer() throws Exception { assertFailed(() -> osv3().imagesV2().importImage("i1", ImageImportOptions.glanceDirect())); }
    public void cacheQueueWithoutMiddleware() throws Exception { assertFailed(() -> osv3().imagesV2().cache().queue("i1")); }
    public void addLocationOnOldServer() throws Exception { assertFailed(() -> osv3().imagesV2().addLocation("i1", "file:///x", null)); }
}
