package org.openstack4j.api.image.v2.ext;

import org.openstack4j.model.common.ActionResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

/** A webob JSON fault (top-level message with HTML line breaks, no Designate type) keeps the status line as its fault. */
@Test(suiteName = "Image/V2/Ext/JsonFault")
public class GlanceJsonFaultTests extends AbstractImageExtTest {

    public void webobJsonFaultIsNotUsedAsMessage() throws Exception {
        respondWith(404, "{\"message\": \"The resource could not be found.<br /><br />\\n\\n\\n\", \"code\": \"404 Not Found\", \"title\": \"Not Found\"}");
        ActionResponse response = osv3().imagesV2().cache().queue("i1");
        takeRequest();
        Assert.assertFalse(response.isSuccess());
        Assert.assertFalse(String.valueOf(response.getFault()).contains("<br"), response.getFault());
    }
}
