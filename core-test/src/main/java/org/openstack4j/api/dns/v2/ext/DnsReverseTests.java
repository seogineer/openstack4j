package org.openstack4j.api.dns.v2.ext;

import java.util.List;

import org.openstack4j.model.dns.v2.ext.ReverseFloatingIp;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "DNS/Reverse")
public class DnsReverseTests extends AbstractDnsExtTest {

    private static final String FIP = "{\"id\": \"RegionOne:c5d53a68-a5aa-4f80-bd6e-4c1bb4d0d1b4\", \"ptrdname\": \"smtp.example.com.\", \"description\": \"mail\","
            + " \"ttl\": 600, \"address\": \"172.24.4.10\", \"status\": \"ACTIVE\", \"action\": \"NONE\", \"links\": {}}";

    public void reverseFloatingIps() throws Exception {
        respondWith(200, "{\"floatingips\": [" + FIP + "], \"links\": {}}");
        respondWith(200, FIP);
        respondWith(202, FIP);
        respondWith(202, FIP.replace("\"smtp.example.com.\"", "null"));

        List<? extends ReverseFloatingIp> all = osv3().dns().reverseFloatingIps().list();
        osv3().dns().reverseFloatingIps().get("RegionOne", "c5d53a68-a5aa-4f80-bd6e-4c1bb4d0d1b4");
        ReverseFloatingIp set = osv3().dns().reverseFloatingIps().set("RegionOne", "c5d53a68-a5aa-4f80-bd6e-4c1bb4d0d1b4", "smtp.example.com.", "mail", 600);
        Assert.assertTrue(osv3().dns().reverseFloatingIps().unset("RegionOne", "c5d53a68-a5aa-4f80-bd6e-4c1bb4d0d1b4").isSuccess());

        expect("GET", "/v2/reverse/floatingips");
        expect("GET", "/v2/reverse/floatingips/RegionOne:c5d53a68-a5aa-4f80-bd6e-4c1bb4d0d1b4");
        Assert.assertEquals(body(expect("PATCH", "/v2/reverse/floatingips/RegionOne:c5d53a68-a5aa-4f80-bd6e-4c1bb4d0d1b4")).toString(),
                "{\"ptrdname\":\"smtp.example.com.\",\"description\":\"mail\",\"ttl\":600}");
        Assert.assertEquals(body(expect("PATCH", "/v2/reverse/floatingips/RegionOne:c5d53a68-a5aa-4f80-bd6e-4c1bb4d0d1b4")).toString(), "{\"ptrdname\":null}");
        Assert.assertEquals(all.get(0).getAddress(), "172.24.4.10");
        Assert.assertEquals(set.getTtl(), Integer.valueOf(600));
    }
}
