package org.openstack4j.api.heat.ext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openstack4j.model.common.ActionResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Regressions from the Heat final review: resource type listing with descriptions and the 404 rules. */
@Test(suiteName = "Heat/Ext/ReviewFixes")
public class HeatReviewFixTests extends AbstractHeatExtTest {

    private static final String NOT_FOUND = "{\"explanation\": \"The resource could not be found.\", \"code\": 404, \"error\": {\"message\": \"The Stack (missing) could not be found.\", \"type\": \"EntityNotFound\"}, \"title\": \"Not Found\"}";

    /** with_description=true returns objects instead of names. */
    public void resourceTypesWithDescriptions() throws Exception {
        respondWith(200, "{\"resource_types\": [{\"resource_type\": \"OS::Heat::RandomString\", \"description\": \"A resource which generates a random string.\"}, \"OS::Heat::None\"]}");
        List<String> types = osv3().heat().resourceTypes().list(java.util.Map.of("with_description", "true"));
        takeRequest();
        Assert.assertEquals(types, List.of("OS::Heat::RandomString", "OS::Heat::None"));
    }

    public void deleteMissingStackReportsFailureWithoutDelete() throws Exception {
        respondWith(404, NOT_FOUND);
        ActionResponse response = osv3().heat().stacks().delete("missing");
        expect("GET", "/stacks/missing");
        Assert.assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "no DELETE expected");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getCode(), 404);
    }

    public void listsRaiseOn404() throws Exception {
        for (Runnable call : new Runnable[] {() -> osv3().heat().stacks().outputs("missing", "x"), () -> osv3().heat().stacks().snapshots("missing", "x")}) {
            respondWith(404, NOT_FOUND);
            try {
                call.run();
                Assert.fail("expected the 404 to surface");
            } catch (RuntimeException expected) {
                Assert.assertTrue(String.valueOf(expected.getMessage()).contains("not be found") || String.valueOf(expected.getMessage()).contains("Not Found"), expected.getMessage());
            }
            takeRequest();
        }
    }

    public void singleGetsReturnNull() throws Exception {
        respondWith(404, NOT_FOUND);
        respondWith(404, NOT_FOUND);
        Assert.assertNull(osv3().heat().stacks().output("s1", "x", "missing"));
        Assert.assertNull(osv3().heat().stacks().getSnapshot("s1", "x", "missing"));
        takeRequest();
        takeRequest();
    }

    public void actionsReportFailure() throws Exception {
        respondWith(Collections.singletonMap("Content-Type", "application/json"), 404, NOT_FOUND);
        ActionResponse response = osv3().heat().stacks().deleteSnapshot("s1", "x", "missing");
        takeRequest();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getCode(), 404);
    }
}
