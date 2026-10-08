package org.openstack4j.api.trove.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Trove/AdminDatastores")
public class TroveAdminDatastoreTests extends AbstractTroveExtTest {

    private static final String P = "/v1.0/26decac97b67478f9f64ff2c2c1b778e";

    public void datastoresAndLimits() throws Exception {
        respondWith(202);
        respondWith(200, "{\"version\": {\"id\": \"v1\", \"name\": \"5.7\", \"version\": \"5.7\"}}");
        respondWith(200, "{\"configuration-parameters\": [{\"name\": \"autocommit\", \"type\": \"integer\", \"min\": 0, \"max\": 1}]}");
        respondWith(200, "{\"name\": \"wait_timeout\", \"type\": \"integer\", \"max\": 31536000}");
        respondWith(200, "{\"configuration-parameters\": []}");
        respondWith(200, "{\"limits\": [{\"max_backups\": 50, \"max_instances\": 10, \"verb\": \"ABSOLUTE\"}]}");

        var ext = osv3().trove().datastoresExt();
        Assert.assertTrue(ext.deleteDatastore("mysql").isSuccess());
        Map<String, Object> version = ext.getVersion("v1");
        List<Map<String, Object>> parameters = ext.listParameters("mysql", "5.7");
        Map<String, Object> parameter = ext.getParameter("mysql", "5.7", "wait_timeout");
        ext.listParameters("v1");
        List<Map<String, Object>> limits = ext.limits();

        expect("DELETE", P + "/datastores/mysql");
        expect("GET", P + "/datastores/versions/v1");
        expect("GET", P + "/datastores/mysql/versions/5.7/parameters");
        expect("GET", P + "/datastores/mysql/versions/5.7/parameters/wait_timeout");
        expect("GET", P + "/datastores/versions/v1/parameters");
        expect("GET", P + "/limits");
        Assert.assertEquals(version.get("name"), "5.7");
        Assert.assertEquals(parameters.get(0).get("name"), "autocommit");
        Assert.assertEquals(parameter.get("max"), 31536000);
        Assert.assertEquals(limits.get(0).get("max_instances"), 10);
    }

    public void admin() throws Exception {
        respondWith(200, "{\"instances\": [{\"id\": \"i1\", \"name\": \"db1\", \"server\": {\"host\": \"compute-001\"}}]}");
        respondWith(200, "{\"instance\": {\"id\": \"i1\", \"deleted\": false}}");
        for (int i = 0; i < 5; i++)
            respondWith(202);
        respondWith(200, "{\"root_history\": {\"enabled\": \"2019-12-24T03:10:16\", \"id\": \"i1\", \"user\": \"u1\"}}");
        respondWith(200, "{\"versions\": [{\"id\": \"v1\", \"datastore_name\": \"mariadb\", \"active\": true}]}");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"configuration-parameters\": [{\"name\": \"connect_timeout\", \"max\": 65535}]}");
        respondWith(200, "{\"name\": \"connect_timeout\", \"restart_required\": true}");
        respondWith(204);
        respondWith(200, "{\"quotas\": [{\"in_use\": 5, \"limit\": 15, \"reserved\": 0, \"resource\": \"instances\"}]}");
        respondWith(200, "{\"quotas\": {\"instances\": 10, \"backups\": 30}}");

        var admin = osv3().trove().troveAdmin();
        List<Map<String, Object>> instances = admin.listInstances(Map.of("deleted", "false"));
        admin.getInstance("i1");
        Assert.assertTrue(admin.stop("i1").isSuccess());
        Assert.assertTrue(admin.reboot("i1").isSuccess());
        Assert.assertTrue(admin.migrate("i1", "compute-001").isSuccess());
        Assert.assertTrue(admin.resetTaskStatus("i1").isSuccess());
        Assert.assertTrue(admin.rebuild("i1", "img1").isSuccess());
        Map<String, Object> history = admin.rootHistory("i1");
        admin.listDatastoreVersions();
        Assert.assertTrue(admin.createDatastoreVersion(Map.of("datastore_name", "mysql", "name", "test", "image_tags", List.of("trove"))).isSuccess());
        Assert.assertTrue(admin.updateDatastoreVersion("v1", Map.of("active", true)).isSuccess());
        Assert.assertTrue(admin.deleteDatastoreVersion("v1").isSuccess());
        List<Map<String, Object>> created = admin.createParameter("v1", Map.of("name", "connect_timeout", "data_type", "integer"));
        Map<String, Object> updated = admin.updateParameter("v1", "connect_timeout", Map.of("restart_required", 1));
        Assert.assertTrue(admin.deleteParameter("v1", "connect_timeout").isSuccess());
        List<Map<String, Object>> quotas = admin.getQuotas("p2");
        Map<String, Object> newQuotas = admin.updateQuotas("p2", Map.of("instances", 10));

        expect("GET", P + "/mgmt/instances?deleted=false");
        expect("GET", P + "/mgmt/instances/i1");
        Assert.assertEquals(body(expect("POST", P + "/mgmt/instances/i1/action")).toString(), "{\"stop\":{}}");
        Assert.assertEquals(body(expect("POST", P + "/mgmt/instances/i1/action")).toString(), "{\"reboot\":{}}");
        Assert.assertEquals(body(expect("POST", P + "/mgmt/instances/i1/action")).toString(), "{\"migrate\":{\"host\":\"compute-001\"}}");
        Assert.assertEquals(body(expect("POST", P + "/mgmt/instances/i1/action")).toString(), "{\"reset-task-status\":{}}");
        Assert.assertEquals(body(expect("POST", P + "/mgmt/instances/i1/action")).toString(), "{\"rebuild\":{\"image_id\":\"img1\"}}");
        expect("GET", P + "/mgmt/instances/i1/root");
        expect("GET", P + "/mgmt/datastore-versions");
        Assert.assertEquals(body(expect("POST", P + "/mgmt/datastore-versions")).get("version").get("datastore_name").asText(), "mysql");
        Assert.assertEquals(body(expect("PATCH", P + "/mgmt/datastore-versions/v1")).toString(), "{\"active\":true}");
        expect("DELETE", P + "/mgmt/datastore-versions/v1");
        RecordedRequest param = expect("POST", P + "/mgmt/datastores/versions/v1/parameters");
        Assert.assertEquals(body(param).get("configuration-parameter").get("data_type").asText(), "integer");
        Assert.assertEquals(body(expect("PUT", P + "/mgmt/datastores/versions/v1/parameters/connect_timeout")).toString(), "{\"configuration-parameter\":{\"restart_required\":1}}");
        expect("DELETE", P + "/mgmt/datastores/versions/v1/parameters/connect_timeout");
        expect("GET", P + "/mgmt/quotas/p2");
        Assert.assertEquals(body(expect("PUT", P + "/mgmt/quotas/p2")).toString(), "{\"quotas\":{\"instances\":10}}");
        Assert.assertEquals(((Map<?, ?>) instances.get(0).get("server")).get("host"), "compute-001");
        Assert.assertEquals(history.get("user"), "u1");
        Assert.assertEquals(created.get(0).get("max"), 65535);
        Assert.assertEquals(updated.get("restart_required"), Boolean.TRUE);
        Assert.assertEquals(quotas.get(0).get("limit"), 15);
        Assert.assertEquals(newQuotas.get("backups"), 30);
    }
}
