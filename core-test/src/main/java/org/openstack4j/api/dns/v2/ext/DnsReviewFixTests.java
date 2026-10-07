package org.openstack4j.api.dns.v2.ext;

import org.openstack4j.model.common.ActionResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "DNS/ReviewFixes")
public class DnsReviewFixTests extends AbstractDnsExtTest {

    public void importSendsContentLengthNotChunked() throws Exception {
        respondWith(202, "{\"id\": \"i1\", \"status\": \"PENDING\"}");
        osv3().dns().zoneFiles().importZone("$ORIGIN example.com.\n");
        var request = takeRequest();
        Assert.assertEquals(request.getHeader("Content-Length"), "21");
        Assert.assertNull(request.getHeader("Transfer-Encoding"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void zoneTaskRejectsSlashInId() {
        osv3().dns().zones().abandon("a/b");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void recordsetFilterRejectsQueryInZoneId() {
        osv3().dns().recordsets().list("z?x=1", java.util.Map.of());
    }

    public void designateFaultMessageStillUsed() throws Exception {
        respondWith(404, "{\"code\": 404, \"type\": \"zone_not_found\", \"message\": \"Could not find Zone\"}");
        ActionResponse response = osv3().dns().zoneShares().delete("z1", "s1");
        takeRequest();
        Assert.assertEquals(response.getFault(), "Could not find Zone");
    }
}
