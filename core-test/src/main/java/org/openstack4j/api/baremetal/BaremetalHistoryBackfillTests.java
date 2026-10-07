package org.openstack4j.api.baremetal;

import java.util.Map;

import org.openstack4j.model.baremetal.options.AllocationCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Baremetal/HistoryBackfill")
public class BaremetalHistoryBackfillTests extends AbstractBaremetalTest {

    public void historyWithFiltersSendsDetail() throws Exception {
        respondWith(200, "{\"history\": [{\"uuid\": \"e2\", \"event\": \"deploy\", \"event_type\": \"PROVISIONING\", \"user\": \"admin\"}]}");
        var history = osv3().baremetal().nodes().listHistory("n1", Map.of("marker", "e1", "limit", "10"));
        String path = decodedPath(takeRequest());
        Assert.assertTrue(path.contains("/v1/nodes/n1/history?") && path.contains("detail=true") && path.contains("marker=e1") && path.contains("limit=10"), path);
        Assert.assertEquals(history.get(0).get("event_type"), "PROVISIONING");
    }

    public void backfillAllocationSendsOnlyNode() throws Exception {
        respondWith(201, "{\"uuid\": \"a1\", \"node_uuid\": \"6d85703a-565d-469a-96ce-30b6de53079d\", \"state\": \"active\"}");
        osv3().baremetal().allocations().create(AllocationCreate.backfill("6d85703a-565d-469a-96ce-30b6de53079d").name("a1"));
        Assert.assertEquals(body(expect("POST", "/v1/allocations")).toString(), "{\"node\":\"6d85703a-565d-469a-96ce-30b6de53079d\",\"name\":\"a1\"}");
    }
}
