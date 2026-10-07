package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.dns.v2.ext.Blacklist;
import org.openstack4j.model.dns.v2.ext.Tld;
import org.openstack4j.model.dns.v2.ext.TsigKey;
import org.openstack4j.model.dns.v2.options.BlacklistCreate;
import org.openstack4j.model.dns.v2.options.BlacklistUpdate;
import org.openstack4j.model.dns.v2.options.TldCreate;
import org.openstack4j.model.dns.v2.options.TldUpdate;
import org.openstack4j.model.dns.v2.options.TsigKeyCreate;
import org.openstack4j.model.dns.v2.options.TsigKeyUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "DNS/Admin")
public class DnsAdminTests extends AbstractDnsExtTest {

    private static final String TLD = "{\"id\": \"5fa28ce7-e59a-4bb8-bd56-b6bd3fa47e2a\", \"name\": \"com\", \"description\": \"tld description\", \"links\": {}}";
    private static final String TSIG = "{\"id\": \"8add45a0-3c5a-4d1d-a8a9-2e4a7d6e6a2c\", \"name\": \"Example key\", \"algorithm\": \"hmac-sha256\","
            + " \"secret\": \"SomeSecretKey\", \"scope\": \"POOL\", \"resource_id\": \"6ca6baef-3305-4ad0-a52b-a82df5752b62\", \"links\": {}}";
    private static final String BLACKLIST = "{\"id\": \"b8dda1a6-a5a1-4c9e-92b4-ff8e1a2f0e5d\", \"pattern\": \"^([A-Za-z0-9_\\\\-]+\\\\.)*example\\\\.com\\\\.$\", \"description\": \"no example\"}";

    public void tlds() throws Exception {
        respondWith(201, TLD);
        respondWith(200, "{\"tlds\": [" + TLD + "], \"links\": {}}");
        respondWith(200, TLD);
        respondWith(200, TLD);
        respondWith(204);

        Tld tld = osv3().dns().tlds().create(TldCreate.create("com").description("tld description"));
        List<? extends Tld> all = osv3().dns().tlds().list(Map.of("name", "com"));
        osv3().dns().tlds().get(tld.getId());
        osv3().dns().tlds().update(tld.getId(), TldUpdate.create().description("changed"));
        Assert.assertTrue(osv3().dns().tlds().delete(tld.getId()).isSuccess());

        Assert.assertEquals(body(expect("POST", "/v2/tlds")).toString(), "{\"name\":\"com\",\"description\":\"tld description\"}");
        expect("GET", "/v2/tlds?name=com");
        expect("GET", "/v2/tlds/5fa28ce7-e59a-4bb8-bd56-b6bd3fa47e2a");
        Assert.assertEquals(body(expect("PATCH", "/v2/tlds/5fa28ce7-e59a-4bb8-bd56-b6bd3fa47e2a")).toString(), "{\"description\":\"changed\"}");
        expect("DELETE", "/v2/tlds/5fa28ce7-e59a-4bb8-bd56-b6bd3fa47e2a");
        Assert.assertEquals(all.get(0).getName(), "com");
    }

    public void tsigKeysAndBlacklists() throws Exception {
        respondWith(201, TSIG);
        respondWith(200, TSIG);
        respondWith(201, BLACKLIST);
        respondWith(200, "{\"blacklists\": [" + BLACKLIST + "], \"links\": {}}");
        respondWith(200, BLACKLIST);

        TsigKey key = osv3().dns().tsigKeys().create(TsigKeyCreate.create("Example key", "hmac-sha256", "SomeSecretKey", "POOL", "6ca6baef-3305-4ad0-a52b-a82df5752b62"));
        osv3().dns().tsigKeys().update(key.getId(), TsigKeyUpdate.create().secret("Rotated"));
        Blacklist blacklist = osv3().dns().blacklists().create(BlacklistCreate.create("^example\\.com\\.$"));
        List<? extends Blacklist> blacklists = osv3().dns().blacklists().list();
        osv3().dns().blacklists().update(blacklist.getId(), BlacklistUpdate.create().description("d"));

        var create = body(expect("POST", "/v2/tsigkeys"));
        Assert.assertEquals(create.get("scope").asText(), "POOL");
        Assert.assertEquals(create.get("resource_id").asText(), "6ca6baef-3305-4ad0-a52b-a82df5752b62");
        Assert.assertEquals(body(expect("PATCH", "/v2/tsigkeys/8add45a0-3c5a-4d1d-a8a9-2e4a7d6e6a2c")).toString(), "{\"secret\":\"Rotated\"}");
        Assert.assertEquals(body(expect("POST", "/v2/blacklists")).get("pattern").asText(), "^example\\.com\\.$");
        expect("GET", "/v2/blacklists");
        expect("PATCH", "/v2/blacklists/b8dda1a6-a5a1-4c9e-92b4-ff8e1a2f0e5d");
        Assert.assertEquals(key.getAlgorithm(), "hmac-sha256");
        Assert.assertEquals(blacklists.get(0).getDescription(), "no example");
    }

    public void quotas() throws Exception {
        String quotas = "{\"api_export_size\": 1000, \"recordset_records\": 20, \"zone_records\": 500, \"zone_recordsets\": 500, \"zones\": 10}";
        respondWith(200, quotas);
        respondWith(200, quotas.replace("\"zones\": 10", "\"zones\": 20"));
        respondWith(204);

        Map<String, Integer> current = osv3().dns().quotas().get("p1");
        Map<String, Integer> updated = osv3().dns().quotas().update("p1", Map.of("zones", 20));
        Assert.assertTrue(osv3().dns().quotas().reset("p1").isSuccess());

        expect("GET", "/v2/quotas/p1");
        Assert.assertEquals(body(expect("PATCH", "/v2/quotas/p1")).toString(), "{\"zones\":20}");
        expect("DELETE", "/v2/quotas/p1");
        Assert.assertEquals(current.get("zones"), Integer.valueOf(10));
        Assert.assertEquals(updated.get("zones"), Integer.valueOf(20));
    }
}
