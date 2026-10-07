package org.openstack4j.api.baremetal;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.model.common.ActionResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/ReviewFixes")
public class BaremetalReviewFixTests extends AbstractBaremetalTest {

    private static final String MISSING = "{\"error_message\": \"{\\\"faultstring\\\": \\\"Node n9 could not be found.\\\", \\\"faultcode\\\": \\\"Client\\\", \\\"debuginfo\\\": null}\"}";

    public void actionFailsOnGatewayTimeout() throws Exception {
        respondWith(504, "{\"error_message\": \"{\\\"faultstring\\\": \\\"No free conductor workers available\\\", \\\"faultcode\\\": \\\"Server\\\", \\\"debuginfo\\\": null}\"}");
        ActionResponse response = osv3().baremetal().nodes().setPowerState("n1", "power on");
        takeRequest();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getCode(), 504);
        Assert.assertEquals(response.getFault(), "No free conductor workers available");
    }

    public void addTraitAndAttachVifFailOnBadGateway() throws Exception {
        respondWith(502, "{\"error_message\": \"bad gateway\"}");
        respondWith(504, "{\"error_message\": \"timeout\"}");
        Assert.assertEquals(osv3().baremetal().nodes().addTrait("n1", "CUSTOM_A").getCode(), 502);
        Assert.assertFalse(osv3().baremetal().nodes().attachVif("n1", "v1").isSuccess());
        takeRequest();
        takeRequest();
    }

    public void actionFaultIsTheIronicFaultstring() throws Exception {
        respondWith(404, MISSING);
        ActionResponse response = osv3().baremetal().nodes().setMaintenance("n9", "x");
        takeRequest();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getFault(), "Node n9 could not be found.");
    }

    public void exceptionMessageIsTheIronicFaultstring() throws Exception {
        respondWith(404, MISSING);
        try {
            osv3().baremetal().nodes().getStates("n9");
            Assert.fail("expected a ResponseException");
        } catch (ResponseException e) {
            Assert.assertEquals(e.getMessage(), "Node n9 could not be found.");
        } finally {
            takeRequest();
        }
    }

    public void plainErrorMessageIsKept() throws Exception {
        respondWith(400, "{\"error_message\": \"plain text\"}");
        ActionResponse response = osv3().baremetal().nodes().setMaintenance("n1", "x");
        takeRequest();
        Assert.assertEquals(response.getFault(), "plain text");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void blankIdentIsRejected() {
        osv3().baremetal().nodes().get(" ");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void identWithSlashIsRejected() {
        osv3().baremetal().ports().delete("a/b");
    }

    @Test(expectedExceptions = MicroVersionException.class)
    public void negotiateWithoutRootDocumentFailsClearly() throws Exception {
        respondWith(java.util.Map.of("Content-Type", "text/html"), 404, "<html><body>404 Not Found</body></html>");
        try {
            osv3().baremetal().microVersions().negotiate();
        } finally {
            takeRequest();
        }
    }
}
