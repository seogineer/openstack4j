package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.manila.ext.ShareReplica;
import org.openstack4j.model.manila.ext.SnapshotInstance;
import org.openstack4j.model.manila.ext.options.ShareReplicaCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/ReplicasSnapshots")
public class ShareReplicaSnapshotTests extends AbstractManilaExtTest {

    private static final String P = "/v2/b80f8d4e28b74188858b654cb1fccf7d";
    private static final String REPLICA = "{\"id\": \"r1\", \"share_id\": \"s1\", \"status\": \"creating\", \"replica_state\": \"out_of_sync\","
            + " \"availability_zone\": \"nova\", \"share_network_id\": null, \"host\": \"h@b#p\", \"cast_rules_to_readonly\": true}";

    public void replicas() throws Exception {
        respondWith(202, "{\"share_replica\": " + REPLICA + "}");
        respondWith(200, "{\"share_replicas\": [" + REPLICA + "]}");
        respondWith(200, "{\"share_replica\": " + REPLICA + "}");
        for (int i = 0; i < 6; i++)
            respondWith(202);
        respondWith(200, "{\"export_locations\": [{\"id\": \"e1\", \"path\": \"10.0.0.1:/r1\", \"preferred\": true}]}");
        respondWith(200, "{\"metadata\": {\"k\": \"v\"}}");
        respondWith(202);

        var replicas = osv3().share().shareReplicas();
        ShareReplica created = replicas.create(ShareReplicaCreate.create("s1").availabilityZone("nova"));
        List<? extends ShareReplica> all = replicas.listDetail("s1");
        replicas.get("r1");
        Assert.assertTrue(replicas.promote("r1", null).isSuccess());
        Assert.assertTrue(replicas.promote("r1", 30).isSuccess());
        Assert.assertTrue(replicas.resync("r1").isSuccess());
        Assert.assertTrue(replicas.resetStatus("r1", "available").isSuccess());
        Assert.assertTrue(replicas.resetReplicaState("r1", "in_sync").isSuccess());
        Assert.assertTrue(replicas.forceDelete("r1").isSuccess());
        replicas.listExportLocations("r1");
        Map<String, String> metadata = replicas.setMetadata("r1", Map.of("k", "v"));
        Assert.assertTrue(replicas.delete("r1").isSuccess());

        RecordedRequest create = expect("POST", P + "/share-replicas");
        Assert.assertEquals(body(create).toString(), "{\"share_replica\":{\"share_id\":\"s1\",\"availability_zone\":\"nova\"}}");
        Assert.assertEquals(create.getHeader("X-OpenStack-Manila-API-Version"), "2.56");
        expect("GET", P + "/share-replicas/detail?share_id=s1");
        expect("GET", P + "/share-replicas/r1");
        Assert.assertEquals(body(expect("POST", P + "/share-replicas/r1/action")).toString(), "{\"promote\":null}");
        RecordedRequest promote = expect("POST", P + "/share-replicas/r1/action");
        Assert.assertEquals(body(promote).toString(), "{\"promote\":{\"quiesce_wait_time\":30}}");
        Assert.assertEquals(promote.getHeader("X-OpenStack-Manila-API-Version"), "2.75");
        Assert.assertEquals(body(expect("POST", P + "/share-replicas/r1/action")).toString(), "{\"resync\":null}");
        Assert.assertEquals(body(expect("POST", P + "/share-replicas/r1/action")).toString(), "{\"reset_status\":{\"status\":\"available\"}}");
        Assert.assertEquals(body(expect("POST", P + "/share-replicas/r1/action")).toString(), "{\"reset_replica_state\":{\"replica_state\":\"in_sync\"}}");
        Assert.assertEquals(body(expect("POST", P + "/share-replicas/r1/action")).toString(), "{\"force_delete\":null}");
        expect("GET", P + "/share-replicas/r1/export-locations");
        Assert.assertEquals(expect("POST", P + "/share-replicas/r1/metadata").getHeader("X-OpenStack-Manila-API-Version"), "2.95");
        expect("DELETE", P + "/share-replicas/r1");
        Assert.assertEquals(created.getReplicaState(), "out_of_sync");
        Assert.assertEquals(all.get(0).isCastRulesToReadonly(), Boolean.TRUE);
        Assert.assertEquals(metadata.get("k"), "v");
    }

    public void snapshotExtensions() throws Exception {
        respondWith(200, "{\"metadata\": {\"project\": \"x\"}}");
        respondWith(200, "{\"meta\": {\"project\": \"x\"}}");
        respondWith(200, "{\"metadata\": {\"project\": \"y\"}}");
        respondWith(200);
        respondWith(202, "{\"snapshot\": {\"id\": \"sn2\", \"share_id\": \"s1\", \"name\": \"managed\", \"status\": \"manage_starting\"}}");
        respondWith(202);
        respondWith(200, "{\"snapshot_instances\": [{\"id\": \"si1\", \"snapshot_id\": \"sn1\", \"status\": \"available\", \"share_instance_id\": \"i1\"}]}");
        respondWith(200, "{\"snapshot_instance\": {\"id\": \"si1\", \"snapshot_id\": \"sn1\", \"status\": \"available\", \"provider_location\": \"/snap\"}}");
        respondWith(202);

        var ext = osv3().share().snapshotsExt();
        Map<String, String> metadata = ext.getMetadata("sn1");
        String item = ext.getMetadataItem("sn1", "project");
        ext.replaceMetadata("sn1", Map.of("project", "y"));
        Assert.assertTrue(ext.deleteMetadataItem("sn1", "project").isSuccess());
        var managed = ext.manage(Map.of("share_id", "s1", "provider_location", "/snap"));
        Assert.assertTrue(ext.unmanage("sn1").isSuccess());
        List<? extends SnapshotInstance> instances = ext.listInstancesDetail(Map.of("snapshot_id", "sn1"));
        SnapshotInstance instance = ext.getInstance("si1");
        Assert.assertTrue(ext.resetInstanceStatus("si1", "error").isSuccess());

        Assert.assertEquals(expect("GET", P + "/snapshots/sn1/metadata").getHeader("X-OpenStack-Manila-API-Version"), "2.73");
        expect("GET", P + "/snapshots/sn1/metadata/project");
        Assert.assertEquals(body(expect("PUT", P + "/snapshots/sn1/metadata")).toString(), "{\"metadata\":{\"project\":\"y\"}}");
        expect("DELETE", P + "/snapshots/sn1/metadata/project");
        RecordedRequest manage = expect("POST", P + "/snapshots/manage");
        Assert.assertEquals(manage.getHeader("X-OpenStack-Manila-API-Version"), "2.12");
        Assert.assertEquals(body(manage).get("snapshot").get("provider_location").asText(), "/snap");
        Assert.assertEquals(body(expect("POST", P + "/snapshots/sn1/action")).toString(), "{\"unmanage\":null}");
        expect("GET", P + "/snapshot-instances/detail?snapshot_id=sn1");
        expect("GET", P + "/snapshot-instances/si1");
        Assert.assertEquals(body(expect("POST", P + "/snapshot-instances/si1/action")).toString(), "{\"reset_status\":{\"status\":\"error\"}}");
        Assert.assertEquals(metadata.get("project"), "x");
        Assert.assertEquals(item, "x");
        Assert.assertEquals(managed.getId(), "sn2");
        Assert.assertEquals(managed.getStatus(), "manage_starting");
        Assert.assertEquals(instances.get(0).getShareInstanceId(), "i1");
        Assert.assertEquals(instance.getProviderLocation(), "/snap");
    }
}
