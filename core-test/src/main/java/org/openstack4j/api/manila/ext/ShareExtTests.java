package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.manila.Access;
import org.openstack4j.model.manila.Share;
import org.openstack4j.model.manila.ext.ExportLocation;
import org.openstack4j.model.manila.ext.ShareAccessRule;
import org.openstack4j.model.manila.ext.options.ShareAccessCreate;
import org.openstack4j.model.manila.ext.options.ShareMigration;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/SharesExt")
public class ShareExtTests extends AbstractManilaExtTest {

    private static final String P = "/v2/b80f8d4e28b74188858b654cb1fccf7d";
    private static final String EL = "{\"path\": \"10.254.0.3:/shares/share-e1c2\", \"share_instance_id\": \"e1c2\", \"is_admin_only\": false,"
            + " \"id\": \"b6bd76ce\", \"preferred\": true}";
    private static final String RULE = "{\"access_level\": \"rw\", \"state\": \"active\", \"id\": \"507bf114\", \"share_id\": \"s1\", \"access_type\": \"ip\","
            + " \"access_to\": \"10.0.0.0/24\", \"access_key\": null, \"metadata\": {\"key1\": \"value1\"}}";

    public void exportLocations() throws Exception {
        respondWith(200, "{\"export_locations\": [" + EL + "]}");
        respondWith(200, "{\"export_location\": " + EL + "}");
        respondWith(200, "{\"metadata\": {\"project\": \"my_app\"}}");
        respondWith(200, "{\"metadata\": {\"project\": \"my_app\", \"aim\": \"doc\"}}");
        respondWith(200, "{\"metadata\": {\"aim\": \"doc\"}}");
        respondWith(200, "{\"meta\": {\"aim\": \"doc\"}}");
        respondWith(200);
        respondWith(200, "{\"export_locations\": [" + EL + "]}");

        var ext = osv3().share().sharesExt();
        List<? extends ExportLocation> locations = ext.listExportLocations("s1");
        ExportLocation one = ext.getExportLocation("s1", "b6bd76ce");
        Map<String, String> metadata = ext.getExportLocationMetadata("s1", "b6bd76ce");
        ext.setExportLocationMetadata("s1", "b6bd76ce", Map.of("aim", "doc"));
        ext.replaceExportLocationMetadata("s1", "b6bd76ce", Map.of("aim", "doc"));
        String item = ext.getExportLocationMetadataItem("s1", "b6bd76ce", "aim");
        Assert.assertTrue(ext.deleteExportLocationMetadataItem("s1", "b6bd76ce", "aim").isSuccess());
        ext.listInstanceExportLocations("e1c2");

        Assert.assertEquals(expect("GET", P + "/shares/s1/export_locations").getHeader("X-OpenStack-Manila-API-Version"), "2.9");
        expect("GET", P + "/shares/s1/export_locations/b6bd76ce");
        Assert.assertEquals(expect("GET", P + "/shares/s1/export_locations/b6bd76ce/metadata").getHeader("X-OpenStack-Manila-API-Version"), "2.87");
        Assert.assertEquals(body(expect("POST", P + "/shares/s1/export_locations/b6bd76ce/metadata")).toString(), "{\"metadata\":{\"aim\":\"doc\"}}");
        Assert.assertEquals(body(expect("PUT", P + "/shares/s1/export_locations/b6bd76ce/metadata")).toString(), "{\"metadata\":{\"aim\":\"doc\"}}");
        expect("GET", P + "/shares/s1/export_locations/b6bd76ce/metadata/aim");
        expect("DELETE", P + "/shares/s1/export_locations/b6bd76ce/metadata/aim");
        expect("GET", P + "/share_instances/e1c2/export_locations");
        Assert.assertEquals(locations.get(0).getPath(), "10.254.0.3:/shares/share-e1c2");
        Assert.assertEquals(one.isPreferred(), Boolean.TRUE);
        Assert.assertEquals(metadata.get("project"), "my_app");
        Assert.assertEquals(item, "doc");
    }

    public void shareActions() throws Exception {
        respondWith(200, "{\"access\": " + RULE + "}");
        for (int i = 0; i < 5; i++)
            respondWith(202);
        respondWith(200, "{\"share\": {\"id\": \"s2\", \"name\": \"accounting\", \"status\": \"manage_starting\"}}");
        respondWith(200, "{\"share_instances\": [{\"id\": \"i1\", \"share_id\": \"s1\", \"status\": \"available\"}]}");

        var ext = osv3().share().sharesExt();
        Access access = ext.grantAccess("s1", ShareAccessCreate.create("ip", "10.0.0.0/24").accessLevel("rw").metadata(Map.of("key1", "value1")));
        Assert.assertTrue(ext.revokeAccess("s1", "507bf114").isSuccess());
        Assert.assertTrue(ext.revertToSnapshot("s1", "snap1").isSuccess());
        Assert.assertTrue(ext.softDelete("s1").isSuccess());
        Assert.assertTrue(ext.restore("s1").isSuccess());
        Assert.assertTrue(ext.unmanage("s1").isSuccess());
        Share managed = ext.manage(Map.of("protocol", "nfs", "export_path", "192.162.10.6:/shares/x", "service_host", "manila2@stor#pool"));
        ext.listInstances("s1");

        RecordedRequest grant = expect("POST", P + "/shares/s1/action");
        Assert.assertEquals(body(grant).toString(), "{\"allow_access\":{\"access_type\":\"ip\",\"access_to\":\"10.0.0.0/24\",\"access_level\":\"rw\",\"metadata\":{\"key1\":\"value1\"}}}");
        Assert.assertEquals(grant.getHeader("X-OpenStack-Manila-API-Version"), "2.45");
        Assert.assertEquals(body(expect("POST", P + "/shares/s1/action")).toString(), "{\"deny_access\":{\"access_id\":\"507bf114\"}}");
        RecordedRequest revert = expect("POST", P + "/shares/s1/action");
        Assert.assertEquals(body(revert).toString(), "{\"revert\":{\"snapshot_id\":\"snap1\"}}");
        Assert.assertEquals(revert.getHeader("X-OpenStack-Manila-API-Version"), "2.27");
        Assert.assertEquals(body(expect("POST", P + "/shares/s1/action")).toString(), "{\"soft_delete\":null}");
        Assert.assertEquals(body(expect("POST", P + "/shares/s1/action")).toString(), "{\"restore\":null}");
        Assert.assertEquals(body(expect("POST", P + "/shares/s1/action")).toString(), "{\"unmanage\":null}");
        Assert.assertEquals(body(expect("POST", P + "/shares/manage")).get("share").get("service_host").asText(), "manila2@stor#pool");
        expect("GET", P + "/shares/s1/instances");
        Assert.assertEquals(access.getAccessTo(), "10.0.0.0/24");
        Assert.assertEquals(managed.getId(), "s2");
    }

    public void accessRules() throws Exception {
        respondWith(200, "{\"access_list\": [" + RULE + "]}");
        respondWith(200, "{\"access\": " + RULE + "}");
        respondWith(200, "{\"access\": " + RULE.replace("\"rw\"", "\"ro\"") + "}");
        respondWith(200, "{\"metadata\": {\"key1\": \"value1\", \"key2\": \"v2\"}}");
        respondWith(200);

        var ext = osv3().share().sharesExt();
        List<? extends ShareAccessRule> rules = ext.listAccessRules("s1", Map.of("access_type", "ip"));
        ShareAccessRule rule = ext.getAccessRule("507bf114");
        ShareAccessRule updated = ext.updateAccessRuleLevel("507bf114", "ro");
        Map<String, String> metadata = ext.updateAccessRuleMetadata("507bf114", Map.of("key2", "v2"));
        Assert.assertTrue(ext.deleteAccessRuleMetadata("507bf114", "key2").isSuccess());

        String list = decodedPath(takeRequest());
        Assert.assertTrue(list.contains("share_id=s1") && list.contains("access_type=ip"), list);
        expect("GET", P + "/share-access-rules/507bf114");
        RecordedRequest level = expect("PUT", P + "/share-access-rules/507bf114");
        Assert.assertEquals(body(level).toString(), "{\"update_access\":{\"access_level\":\"ro\"}}");
        Assert.assertEquals(level.getHeader("X-OpenStack-Manila-API-Version"), "2.88");
        Assert.assertEquals(body(expect("PUT", P + "/share-access-rules/507bf114/metadata")).toString(), "{\"metadata\":{\"key2\":\"v2\"}}");
        expect("DELETE", P + "/share-access-rules/507bf114/metadata/key2");
        Assert.assertEquals(rules.get(0).getState(), "active");
        Assert.assertEquals(rule.getMetadata().get("key1"), "value1");
        Assert.assertEquals(updated.getAccessLevel(), "ro");
        Assert.assertEquals(metadata.get("key2"), "v2");
    }

    public void migrationIsExperimentalBelow296() throws Exception {
        respondWith(202);
        respondWith(200, "{\"total_progress\": 50, \"task_state\": \"migration_driver_in_progress\"}");
        respondWith(202);
        respondWith(202);

        var ext = osv3().share().sharesExt();
        Assert.assertTrue(ext.migrationStart("s1", ShareMigration.to("ubuntu@generic2#GENERIC2", true, true, true, false).newShareTypeId("t2")).isSuccess());
        Map<String, Object> progress = ext.migrationProgress("s1");
        Assert.assertTrue(ext.migrationComplete("s1").isSuccess());
        Assert.assertTrue(ext.migrationCancel("s1").isSuccess());

        RecordedRequest start = expect("POST", P + "/shares/s1/action");
        Assert.assertEquals(start.getHeader("X-OpenStack-Manila-API-Version"), "2.29");
        Assert.assertEquals(start.getHeader("X-OpenStack-Manila-API-Experimental"), "True");
        Assert.assertEquals(body(start).toString(), "{\"migration_start\":{\"host\":\"ubuntu@generic2#GENERIC2\",\"writable\":true,\"preserve_metadata\":true,"
                + "\"preserve_snapshots\":true,\"nondisruptive\":false,\"new_share_type_id\":\"t2\"}}");
        Assert.assertEquals(body(expect("POST", P + "/shares/s1/action")).toString(), "{\"migration_get_progress\":null}");
        Assert.assertEquals(body(expect("POST", P + "/shares/s1/action")).toString(), "{\"migration_complete\":null}");
        Assert.assertEquals(body(expect("POST", P + "/shares/s1/action")).toString(), "{\"migration_cancel\":null}");
        Assert.assertEquals(progress.get("total_progress"), 50);
    }
}
