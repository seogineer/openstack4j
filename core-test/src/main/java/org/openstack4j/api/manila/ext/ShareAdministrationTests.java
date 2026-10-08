package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.manila.AvailabilityZone;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/Administration")
public class ShareAdministrationTests extends AbstractManilaExtTest {

    private static final String P = "/v2/b80f8d4e28b74188858b654cb1fccf7d";

    public void zonesAndServices() throws Exception {
        respondWith(200, "{\"availability_zones\": [{\"name\": \"nova\", \"id\": \"388c983d\", \"created_at\": \"2015-09-18T09:50:55.000000\", \"updated_at\": null}]}");
        respondWith(200, "{\"services\": [{\"id\": 1, \"binary\": \"manila-share\", \"host\": \"manila2@generic1\", \"zone\": \"nova\", \"status\": \"enabled\", \"state\": \"up\"}]}");
        respondWith(200, "{\"host\": \"manila2@generic1\", \"binary\": \"manila-share\", \"disabled\": false}");
        respondWith(200, "{\"host\": \"manila2@generic1\", \"binary\": \"manila-share\", \"disabled\": true, \"disabled_reason\": \"maintenance\"}");
        respondWith(202);

        var admin = osv3().share().administration();
        List<? extends AvailabilityZone> zones = admin.availabilityZones();
        List<? extends org.openstack4j.model.manila.Service> services = admin.services(Map.of("binary", "manila-share"));
        Map<String, Object> enabled = admin.enableService("manila2@generic1", "manila-share");
        Map<String, Object> disabled = admin.disableService("manila2@generic1", "manila-share", "maintenance");
        Assert.assertTrue(admin.ensureShares("manila2@generic1").isSuccess());

        var r = expect("GET", P + "/availability-zones");
        Assert.assertEquals(r.getHeader("X-OpenStack-Manila-API-Version"), "2.7");
        expect("GET", P + "/services?binary=manila-share");
        Assert.assertEquals(body(expect("PUT", P + "/services/enable")).toString(), "{\"host\":\"manila2@generic1\",\"binary\":\"manila-share\"}");
        var d = expect("PUT", P + "/services/disable");
        Assert.assertEquals(body(d).toString(), "{\"host\":\"manila2@generic1\",\"binary\":\"manila-share\",\"disabled_reason\":\"maintenance\"}");
        Assert.assertEquals(d.getHeader("X-OpenStack-Manila-API-Version"), "2.83");
        var e = expect("POST", P + "/services/ensure-shares");
        Assert.assertEquals(body(e).toString(), "{\"ensure_shares\":{\"host\":\"manila2@generic1\"}}");
        Assert.assertEquals(e.getHeader("X-OpenStack-Manila-API-Version"), "2.86");
        Assert.assertEquals(zones.get(0).getName(), "nova");
        Assert.assertEquals(services.get(0).getHost(), "manila2@generic1");
        Assert.assertEquals(enabled.get("disabled"), Boolean.FALSE);
        Assert.assertEquals(disabled.get("disabled_reason"), "maintenance");
    }

    public void quotas() throws Exception {
        String quotas = "{\"quota_set\": {\"id\": \"p1\", \"shares\": 50, \"gigabytes\": 1000, \"snapshots\": 50, \"share_networks\": 10}}";
        respondWith(200, quotas);
        respondWith(200, "{\"quota_set\": {\"id\": \"p1\", \"shares\": {\"in_use\": 2, \"limit\": 50, \"reserved\": 0}}}");
        respondWith(200, quotas);
        respondWith(200, quotas.replace("\"shares\": 50", "\"shares\": 80"));
        respondWith(202);
        respondWith(200, "{\"quota_class_set\": {\"id\": \"default\", \"shares\": 50}}");
        respondWith(200, "{\"quota_class_set\": {\"shares\": 60}}");

        var admin = osv3().share().administration();
        Map<String, Object> current = admin.quotaSet("p1");
        Map<String, Object> detail = admin.quotaSetDetail("p1");
        admin.quotaSetDefaults("p1");
        Map<String, Object> updated = admin.updateQuotaSet("p1", Map.of("shares", 80));
        Assert.assertTrue(admin.deleteQuotaSet("p1").isSuccess());
        Map<String, Object> cls = admin.quotaClassSet("default");
        admin.updateQuotaClassSet("default", Map.of("shares", 60));

        expect("GET", P + "/quota-sets/p1");
        Assert.assertEquals(expect("GET", P + "/quota-sets/p1/detail").getHeader("X-OpenStack-Manila-API-Version"), "2.25");
        expect("GET", P + "/quota-sets/p1/defaults");
        Assert.assertEquals(body(expect("PUT", P + "/quota-sets/p1")).toString(), "{\"quota_set\":{\"shares\":80}}");
        expect("DELETE", P + "/quota-sets/p1");
        expect("GET", P + "/quota-class-sets/default");
        Assert.assertEquals(body(expect("PUT", P + "/quota-class-sets/default")).toString(), "{\"quota_class_set\":{\"shares\":60}}");
        Assert.assertEquals(current.get("shares"), 50);
        Assert.assertEquals(((Map<?, ?>) detail.get("shares")).get("in_use"), 2);
        Assert.assertEquals(updated.get("shares"), 80);
        Assert.assertEquals(cls.get("shares"), 50);
    }

    public void shareTypeAccess() throws Exception {
        respondWith(200, "{\"share_type_access\": [{\"share_type_id\": \"t1\", \"project_id\": \"p2\"}]}");
        List<Map<String, Object>> access = osv3().share().administration().shareTypeAccess("t1");
        expect("GET", P + "/types/t1/share_type_access");
        Assert.assertEquals(access.get(0).get("project_id"), "p2");
    }
}
