package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.dns.v2.Recordset;
import org.openstack4j.model.dns.v2.Zone;
import org.openstack4j.model.dns.v2.ext.Pool;
import org.openstack4j.model.dns.v2.ext.ServiceStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "DNS/Basics")
public class DnsBasicsTests extends AbstractDnsExtTest {

    private static final String ZONE = "{\"id\": \"a86dba58-0043-4cc6-a1bb-69d5e86f3ca3\", \"name\": \"example.org.\", \"email\": \"joe@example.org\", \"ttl\": 7200,"
            + " \"status\": \"ACTIVE\", \"type\": \"PRIMARY\", \"pool_id\": \"572ba08c-d929-4c70-8e42-03824bb24ca2\", \"links\": {}}";

    public void zoneAndRecordsetFilters() throws Exception {
        respondWith(200, "{\"zones\": [" + ZONE + "], \"links\": {}, \"metadata\": {\"total_count\": 1}}");
        respondWith(200, "{\"recordsets\": [{\"id\": \"f7\", \"name\": \"www.example.org.\", \"type\": \"A\", \"records\": [\"10.1.0.2\"]}], \"links\": {}}");
        respondWith(200, "{\"recordsets\": [], \"links\": {}}");

        List<? extends Zone> zones = osv3().dns().zones().list(Map.of("name", "example.org."));
        List<? extends Recordset> inZone = osv3().dns().recordsets().list("a86dba58", Map.of("type", "A"));
        osv3().dns().recordsets().list(Map.of("data", "10.1.0.2"));

        expect("GET", "/v2/zones?name=example.org.");
        expect("GET", "/v2/zones/a86dba58/recordsets?type=A");
        expect("GET", "/v2/recordsets?data=10.1.0.2");
        Assert.assertEquals(zones.get(0).getName(), "example.org.");
        Assert.assertEquals(inZone.get(0).getRecords(), List.of("10.1.0.2"));
    }

    public void zoneTasks() throws Exception {
        respondWith(204);
        respondWith(202);
        respondWith(202, ZONE);

        Assert.assertTrue(osv3().dns().zones().abandon("z1").isSuccess());
        Assert.assertTrue(osv3().dns().zones().transferFromMaster("z1").isSuccess());
        Assert.assertTrue(osv3().dns().zones().movePool("z1", "p2").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v2/zones/z1/tasks/abandon")).toString(), "{}");
        Assert.assertEquals(body(expect("POST", "/v2/zones/z1/tasks/xfr")).toString(), "{}");
        Assert.assertEquals(body(expect("POST", "/v2/zones/z1/tasks/pool_move")).toString(), "{\"pool_id\":\"p2\"}");
    }

    public void poolsLimitsServiceStatuses() throws Exception {
        String pool = "{\"id\": \"794ccc2c-d751-44fe-b57f-8894c9f5c842\", \"name\": \"default\", \"description\": null, \"attributes\": {\"service_tier\": \"GOLD\"},"
                + " \"ns_records\": [{\"hostname\": \"ns1.example.org.\", \"priority\": 1}], \"project_id\": null, \"links\": {}}";
        respondWith(200, "{\"pools\": [" + pool + "], \"links\": {}}");
        respondWith(200, pool);
        respondWith(200, "{\"max_page_limit\": 1000, \"max_recordset_name_length\": 255, \"max_zone_records\": 500}");
        respondWith(200, "{\"service_statuses\": [{\"id\": \"s1\", \"hostname\": \"dns-2.example.com.\", \"service_name\": \"central\", \"status\": \"UP\","
                + " \"stats\": {}, \"capabilities\": {}, \"heartbeated_at\": \"2016-03-14T10:48:31.000000\"}], \"links\": {}}");
        respondWith(200, "{\"id\": \"s1\", \"hostname\": \"dns-2.example.com.\", \"service_name\": \"central\", \"status\": \"UP\"}");

        List<? extends Pool> pools = osv3().dns().pools().list();
        Pool one = osv3().dns().pools().get("794ccc2c-d751-44fe-b57f-8894c9f5c842");
        Map<String, Object> limits = osv3().dns().info().limits();
        List<? extends ServiceStatus> statuses = osv3().dns().serviceStatuses().list();
        ServiceStatus status = osv3().dns().serviceStatuses().get("s1");

        expect("GET", "/v2/pools");
        expect("GET", "/v2/pools/794ccc2c-d751-44fe-b57f-8894c9f5c842");
        expect("GET", "/v2/limits");
        expect("GET", "/v2/service_statuses");
        expect("GET", "/v2/service_statuses/s1");
        Assert.assertEquals(pools.get(0).getPoolAttributes().get("service_tier"), "GOLD");
        Assert.assertEquals(one.getNsRecords().get(0).get("hostname"), "ns1.example.org.");
        Assert.assertEquals(limits.get("max_zone_records"), 500);
        Assert.assertEquals(statuses.get(0).getServiceName(), "central");
        Assert.assertEquals(status.getStatus(), "UP");
    }

    public void missingPoolIsNullAndFilteredListRaises() throws Exception {
        respondWith(404, "{\"code\": 404, \"type\": \"pool_not_found\", \"message\": \"Could not find Pool\"}");
        respondWith(404, "{\"code\": 404, \"type\": \"zone_not_found\", \"message\": \"Could not find Zone\"}");
        Assert.assertNull(osv3().dns().pools().get("missing"));
        takeRequest();
        try {
            osv3().dns().recordsets().list("missing", Map.of("type", "A"));
            Assert.fail("expected a ResponseException");
        } catch (org.openstack4j.api.exceptions.ResponseException e) {
            Assert.assertEquals(e.getMessage(), "Could not find Zone");
        } finally {
            takeRequest();
        }
    }
}
