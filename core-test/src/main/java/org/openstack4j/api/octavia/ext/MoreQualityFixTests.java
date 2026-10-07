package org.openstack4j.api.octavia.ext;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Deferred minors from the Octavia review: quota list filters, amphora cert_busy. */
@Test(suiteName = "Octavia/Ext/QualityFixes")
public class MoreQualityFixTests extends AbstractOctaviaExtTest {

    public void quotaListFilters() throws Exception {
        respondWith(200, "{\"quotas\": []}");
        osv3().octavia().quotas().list(Map.of("project_id", "p1"));
        expect("GET", "/v2.0/lbaas/quotas?project_id=p1");
    }

    public void amphoraCertBusy() throws Exception {
        respondWith(200, "{\"amphora\": {\"id\": \"a1\", \"cert_busy\": 1}}");
        Assert.assertEquals(osv3().octavia().amphorae().get("a1").getCertBusy(), Integer.valueOf(1));
        takeRequest();
    }
}
