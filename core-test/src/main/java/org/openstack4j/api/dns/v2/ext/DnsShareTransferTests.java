package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.dns.v2.ext.ZoneShare;
import org.openstack4j.model.dns.v2.ext.ZoneTransferAccept;
import org.openstack4j.model.dns.v2.ext.ZoneTransferRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "DNS/SharesTransfers")
public class DnsShareTransferTests extends AbstractDnsExtTest {

    private static final String SHARE = "{\"id\": \"fd40b017-bf6c-4a8e-8e6c-4c1b0b0bd5f3\", \"zone_id\": \"z1\", \"project_id\": \"p1\", \"target_project_id\": \"p2\", \"links\": {}}";
    private static final String REQUEST = "{\"id\": \"f2ad17b5-807a-423f-a991-e06236c247be\", \"key\": \"9Z2R50Y0\", \"zone_id\": \"z1\", \"zone_name\": \"example.org.\","
            + " \"project_id\": \"p1\", \"target_project_id\": \"p2\", \"description\": \"move\", \"status\": \"ACTIVE\", \"links\": {}}";
    private static final String ACCEPT = "{\"id\": \"581891d5-99f5-47e7-a670-1d7d3c7d2b0b\", \"key\": \"9Z2R50Y0\", \"zone_id\": \"z1\","
            + " \"zone_transfer_request_id\": \"f2ad17b5-807a-423f-a991-e06236c247be\", \"project_id\": \"p2\", \"status\": \"COMPLETE\", \"links\": {}}";

    public void shares() throws Exception {
        respondWith(201, SHARE);
        respondWith(200, "{\"shared_zones\": [" + SHARE + "], \"links\": {}}");
        respondWith(200, SHARE);
        respondWith(204);

        ZoneShare share = osv3().dns().zoneShares().create("z1", "p2");
        List<? extends ZoneShare> all = osv3().dns().zoneShares().list("z1", Map.of("target_project_id", "p2"));
        osv3().dns().zoneShares().get("z1", share.getId());
        Assert.assertTrue(osv3().dns().zoneShares().delete("z1", share.getId()).isSuccess());

        Assert.assertEquals(body(expect("POST", "/v2/zones/z1/shares")).toString(), "{\"target_project_id\":\"p2\"}");
        expect("GET", "/v2/zones/z1/shares?target_project_id=p2");
        expect("GET", "/v2/zones/z1/shares/fd40b017-bf6c-4a8e-8e6c-4c1b0b0bd5f3");
        expect("DELETE", "/v2/zones/z1/shares/fd40b017-bf6c-4a8e-8e6c-4c1b0b0bd5f3");
        Assert.assertEquals(all.get(0).getTargetProjectId(), "p2");
    }

    public void transferRequestAndAccept() throws Exception {
        respondWith(201, REQUEST);
        respondWith(200, "{\"transfer_requests\": [" + REQUEST + "], \"links\": {}}");
        respondWith(200, REQUEST);
        respondWith(200, REQUEST);
        respondWith(204);
        respondWith(201, ACCEPT);
        respondWith(200, "{\"transfer_accepts\": [" + ACCEPT + "], \"links\": {}}");
        respondWith(200, ACCEPT);

        var transfers = osv3().dns().zoneTransfers();
        ZoneTransferRequest request = transfers.createRequest("z1", "p2", "move");
        transfers.listRequests();
        transfers.getRequest(request.getId());
        transfers.updateRequest(request.getId(), null, "moved");
        Assert.assertTrue(transfers.deleteRequest(request.getId()).isSuccess());
        ZoneTransferAccept accept = transfers.accept(request.getId(), request.getKey());
        transfers.listAccepts(Map.of("status", "COMPLETE"));
        transfers.getAccept(accept.getId());

        Assert.assertEquals(body(expect("POST", "/v2/zones/z1/tasks/transfer_requests")).toString(), "{\"target_project_id\":\"p2\",\"description\":\"move\"}");
        expect("GET", "/v2/zones/tasks/transfer_requests");
        expect("GET", "/v2/zones/tasks/transfer_requests/f2ad17b5-807a-423f-a991-e06236c247be");
        Assert.assertEquals(body(expect("PATCH", "/v2/zones/tasks/transfer_requests/f2ad17b5-807a-423f-a991-e06236c247be")).toString(), "{\"description\":\"moved\"}");
        expect("DELETE", "/v2/zones/tasks/transfer_requests/f2ad17b5-807a-423f-a991-e06236c247be");
        Assert.assertEquals(body(expect("POST", "/v2/zones/tasks/transfer_accepts")).toString(),
                "{\"key\":\"9Z2R50Y0\",\"zone_transfer_request_id\":\"f2ad17b5-807a-423f-a991-e06236c247be\"}");
        expect("GET", "/v2/zones/tasks/transfer_accepts?status=COMPLETE");
        expect("GET", "/v2/zones/tasks/transfer_accepts/581891d5-99f5-47e7-a670-1d7d3c7d2b0b");
        Assert.assertEquals(request.getKey(), "9Z2R50Y0");
        Assert.assertEquals(request.getZoneName(), "example.org.");
        Assert.assertEquals(accept.getStatus(), "COMPLETE");
    }

    public void openTransferSendsEmptyBody() throws Exception {
        respondWith(201, REQUEST);
        osv3().dns().zoneTransfers().createRequest("z1", null, null);
        Assert.assertEquals(body(expect("POST", "/v2/zones/z1/tasks/transfer_requests")).toString(), "{}");
    }
}
