package org.openstack4j.api.trove.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.trove.Instance;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Trove/InstancesExt")
public class TroveInstanceExtTests extends AbstractTroveExtTest {

    private static final String P = "/v1.0/26decac97b67478f9f64ff2c2c1b778e";
    private static final String INSTANCE = "{\"id\": \"i1\", \"name\": \"db1\", \"status\": \"ACTIVE\", \"flavor\": {\"id\": \"2\"}}";

    public void detailAndUpdates() throws Exception {
        respondWith(200, "{\"instances\": [" + INSTANCE + "]}");
        for (int i = 0; i < 6; i++)
            respondWith(202);

        var ext = osv3().trove().instancesExt();
        List<? extends Instance> instances = ext.listDetail(Map.of("include_clustered", "true"));
        Assert.assertTrue(ext.rename("i1", "sample_instance").isSuccess());
        Assert.assertTrue(ext.attachConfiguration("i1", "cfg1").isSuccess());
        Assert.assertTrue(ext.detachConfiguration("i1").isSuccess());
        Assert.assertTrue(ext.upgradeDatastoreVersion("i1", "8.0").isSuccess());
        Assert.assertTrue(ext.detachReplica("i1").isSuccess());
        Assert.assertTrue(ext.updateAccess("i1", true, List.of("10.0.0.0/24")).isSuccess());

        expect("GET", P + "/instances/detail?include_clustered=true");
        Assert.assertEquals(body(expect("PUT", P + "/instances/i1")).toString(), "{\"instance\":{\"name\":\"sample_instance\"}}");
        Assert.assertEquals(body(expect("PUT", P + "/instances/i1")).toString(), "{\"instance\":{\"configuration\":\"cfg1\"}}");
        Assert.assertEquals(body(expect("PUT", P + "/instances/i1")).toString(), "{\"instance\":{\"configuration\":null}}");
        Assert.assertEquals(body(expect("PUT", P + "/instances/i1")).toString(), "{\"instance\":{\"datastore_version\":\"8.0\"}}");
        Assert.assertEquals(body(expect("PUT", P + "/instances/i1")).toString(), "{\"instance\":{\"replica_of\":null}}");
        Assert.assertEquals(body(expect("PUT", P + "/instances/i1")).toString(), "{\"instance\":{\"access\":{\"is_public\":true,\"allowed_cidrs\":[\"10.0.0.0/24\"]}}}");
        Assert.assertEquals(instances.get(0).getName(), "db1");
    }

    public void actions() throws Exception {
        for (int i = 0; i < 6; i++)
            respondWith(202);

        var ext = osv3().trove().instancesExt();
        Assert.assertTrue(ext.restart("i1").isSuccess());
        Assert.assertTrue(ext.resizeFlavor("i1", "2").isSuccess());
        Assert.assertTrue(ext.resizeVolume("i1", 4).isSuccess());
        Assert.assertTrue(ext.promoteToReplicaSource("i1").isSuccess());
        Assert.assertTrue(ext.ejectReplicaSource("i1").isSuccess());
        Assert.assertTrue(ext.resetStatus("i1").isSuccess());

        Assert.assertEquals(body(expect("POST", P + "/instances/i1/action")).toString(), "{\"restart\":{}}");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/action")).toString(), "{\"resize\":{\"flavorRef\":\"2\"}}");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/action")).toString(), "{\"resize\":{\"volume\":{\"size\":4}}}");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/action")).toString(), "{\"promote_to_replica_source\":{}}");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/action")).toString(), "{\"eject_replica_source\":{}}");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/action")).toString(), "{\"reset_status\":{}}");
    }

    public void backupsConfigurationLogsSslRoot() throws Exception {
        String log = "{\"name\": \"general\", \"type\": \"USER\", \"status\": \"Partial\", \"published\": \"128\", \"pending\": \"4096\", \"container\": \"data_logs\"}";
        respondWith(200, "{\"backups\": [{\"id\": \"b1\", \"name\": \"snapshot\", \"status\": \"COMPLETED\", \"instance_id\": \"i1\"}]}");
        respondWith(200, "{\"instance\": {\"configuration\": {\"max_connections\": \"100\"}}}");
        respondWith(200, "{\"logs\": [" + log + "]}");
        respondWith(200, "{\"log\": " + log + "}");
        respondWith(200, "{\"log\": " + log + "}");
        respondWith(200, "{\"log\": " + log + "}");
        respondWith(200, "{\"log\": " + log + "}");
        respondWith(200, "{\"log\": " + log + "}");
        respondWith(200, "{\"ssl\": {\"status\": \"on\", \"mode\": \"basic\", \"certificate\": {\"cn\": \"self-signed certificate\"}}}");
        respondWith(202, "{\"ssl\": {\"status\": \"on\", \"mode\": \"basic\", \"restart_required\": false}}");
        respondWith(202, "{\"ssl\": {\"status\": \"off\"}}");
        respondWith(202, "{\"ssl\": {\"status\": \"off\"}}");
        respondWith(200, "{\"rootEnabled\": true}");
        respondWith(200, "{\"user\": {\"name\": \"root\", \"password\": \"q5BX\"}}");
        respondWith(204);

        var ext = osv3().trove().instancesExt();
        List<Map<String, Object>> backups = ext.listBackups("i1");
        Map<String, Object> configuration = ext.configurationDefaults("i1");
        List<Map<String, Object>> logs = ext.listLogs("i1");
        ext.showLog("i1", "general");
        ext.enableLog("i1", "general");
        ext.disableLog("i1", "general");
        ext.publishLog("i1", "general");
        Map<String, Object> discarded = ext.discardLog("i1", "general");
        Map<String, Object> ssl = ext.sslStatus("i1");
        ext.enableSsl("i1", Map.of("mode", "basic"));
        ext.disableSsl("i1");
        ext.rollbackSsl("i1");
        boolean rootEnabled = ext.isRootEnabled("i1");
        Map<String, Object> root = ext.enableRoot("i1", null);
        Assert.assertTrue(ext.disableRoot("i1").isSuccess());

        expect("GET", P + "/instances/i1/backups");
        expect("GET", P + "/instances/i1/configuration");
        expect("GET", P + "/instances/i1/log");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/log")).toString(), "{\"name\":\"general\"}");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/log")).toString(), "{\"name\":\"general\",\"enable\":1}");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/log")).toString(), "{\"name\":\"general\",\"disable\":1}");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/log")).toString(), "{\"name\":\"general\",\"publish\":1}");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/log")).toString(), "{\"name\":\"general\",\"discard\":1}");
        expect("GET", P + "/instances/i1/ssl");
        RecordedRequest enable = expect("POST", P + "/instances/i1/ssl");
        Assert.assertEquals(body(enable).get("ssl").get("enable").asBoolean(), true);
        Assert.assertEquals(body(enable).get("ssl").get("mode").asText(), "basic");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/ssl")).toString(), "{\"ssl\":{\"disable\":true}}");
        Assert.assertEquals(body(expect("POST", P + "/instances/i1/ssl")).toString(), "{\"ssl\":{\"rollback\":true}}");
        expect("GET", P + "/instances/i1/root");
        Assert.assertEquals(expect("POST", P + "/instances/i1/root").getBodySize(), 0);
        expect("DELETE", P + "/instances/i1/root");
        Assert.assertEquals(backups.get(0).get("status"), "COMPLETED");
        Assert.assertEquals(configuration.get("max_connections"), "100");
        Assert.assertEquals(logs.get(0).get("container"), "data_logs");
        Assert.assertEquals(discarded.get("status"), "Partial");
        Assert.assertEquals(ssl.get("status"), "on");
        Assert.assertTrue(rootEnabled);
        Assert.assertEquals(root.get("password"), "q5BX");
    }
}
