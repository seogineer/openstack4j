package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.manila.ext.ShareNetworkSubnet;
import org.openstack4j.model.manila.ext.options.ShareNetworkSubnetCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/NetworksServers")
public class ShareNetworkServerTests extends AbstractManilaExtTest {

    private static final String P = "/v2/b80f8d4e28b74188858b654cb1fccf7d";
    private static final String SUBNET = "{\"id\": \"sub1\", \"availability_zone\": \"manila-zone-0\", \"share_network_id\": \"n1\", \"share_network_name\": \"net_my1\","
            + " \"segmentation_id\": null, \"neutron_subnet_id\": \"ns1\", \"neutron_net_id\": \"nn1\", \"ip_version\": 4, \"cidr\": \"10.0.0.0/24\", \"mtu\": 1500}";

    public void subnets() throws Exception {
        respondWith(200, "{\"share_network_subnet\": " + SUBNET + "}");
        respondWith(200, "{\"share_network_subnets\": [" + SUBNET + "]}");
        respondWith(200, "{\"share_network_subnet\": " + SUBNET + "}");
        respondWith(200, "{\"metadata\": {\"k\": \"v\"}}");
        respondWith(202);

        var subnets = osv3().share().shareNetworkSubnets();
        ShareNetworkSubnet created = subnets.create("n1", ShareNetworkSubnetCreate.create().neutronNetId("nn1").neutronSubnetId("ns1").availabilityZone("manila-zone-0"));
        List<? extends ShareNetworkSubnet> all = subnets.list("n1");
        subnets.get("n1", "sub1");
        Map<String, String> metadata = subnets.getMetadata("n1", "sub1");
        Assert.assertTrue(subnets.delete("n1", "sub1").isSuccess());

        RecordedRequest create = expect("POST", P + "/share-networks/n1/subnets");
        Assert.assertEquals(body(create).toString(), "{\"share-network-subnet\":{\"neutron_net_id\":\"nn1\",\"neutron_subnet_id\":\"ns1\",\"availability_zone\":\"manila-zone-0\"}}");
        Assert.assertEquals(create.getHeader("X-OpenStack-Manila-API-Version"), "2.51");
        expect("GET", P + "/share-networks/n1/subnets");
        expect("GET", P + "/share-networks/n1/subnets/sub1");
        Assert.assertEquals(expect("GET", P + "/share-networks/n1/subnets/sub1/metadata").getHeader("X-OpenStack-Manila-API-Version"), "2.78");
        expect("DELETE", P + "/share-networks/n1/subnets/sub1");
        Assert.assertEquals(created.getShareNetworkName(), "net_my1");
        Assert.assertEquals(all.get(0).getMtu(), Integer.valueOf(1500));
        Assert.assertEquals(metadata.get("k"), "v");
    }

    public void shareServers() throws Exception {
        respondWith(200, "{\"details\": {\"ip\": \"10.254.0.3\", \"username\": \"manila\"}}");
        respondWith(202, "{\"share_server\": {\"id\": \"ss2\", \"host\": \"myhost@mybackend\", \"status\": \"manage_starting\"}}");
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"compatible\": false, \"requested_capabilities\": {}, \"supported_capabilities\": {}}");
        respondWith(202);
        respondWith(200, "{\"total_progress\": 50, \"task_state\": \"migration_driver_in_progress\", \"destination_share_server_id\": \"ss3\"}");
        respondWith(202);
        respondWith(202);

        var ext = osv3().share().shareServersExt();
        Map<String, Object> details = ext.details("ss1");
        var managed = ext.manage(Map.of("host", "myhost@mybackend", "share_network_id", "n1", "identifier", "x"));
        Assert.assertTrue(ext.unmanage("ss1", false).isSuccess());
        Assert.assertTrue(ext.resetStatus("ss1", "active").isSuccess());
        Map<String, Object> check = ext.migrationCheck("ss1", Map.of("host", "foohost2@backend2", "writable", true, "nondisruptive", true, "preserve_snapshots", true));
        Assert.assertTrue(ext.migrationStart("ss1", Map.of("host", "foohost2@backend2", "writable", true, "nondisruptive", false, "preserve_snapshots", true)).isSuccess());
        Map<String, Object> progress = ext.migrationProgress("ss1");
        Assert.assertTrue(ext.migrationComplete("ss1").isSuccess());
        Assert.assertTrue(ext.resetTaskState("ss1", null).isSuccess());

        expect("GET", P + "/share-servers/ss1/details");
        RecordedRequest manage = expect("POST", P + "/share-servers/manage");
        Assert.assertEquals(manage.getHeader("X-OpenStack-Manila-API-Version"), "2.49");
        Assert.assertEquals(body(manage).get("share_server").get("identifier").asText(), "x");
        Assert.assertEquals(body(expect("POST", P + "/share-servers/ss1/action")).toString(), "{\"unmanage\":{\"force\":false}}");
        Assert.assertEquals(body(expect("POST", P + "/share-servers/ss1/action")).toString(), "{\"reset_status\":{\"status\":\"active\"}}");
        RecordedRequest checkRequest = expect("POST", P + "/share-servers/ss1/action");
        Assert.assertEquals(checkRequest.getHeader("X-OpenStack-Manila-API-Experimental"), "True");
        Assert.assertEquals(checkRequest.getHeader("X-OpenStack-Manila-API-Version"), "2.57");
        Assert.assertEquals(expect("POST", P + "/share-servers/ss1/action").getHeader("X-OpenStack-Manila-API-Experimental"), "True");
        Assert.assertEquals(body(expect("POST", P + "/share-servers/ss1/action")).toString(), "{\"migration_get_progress\":null}");
        Assert.assertEquals(body(expect("POST", P + "/share-servers/ss1/action")).toString(), "{\"migration_complete\":null}");
        Assert.assertEquals(body(expect("POST", P + "/share-servers/ss1/action")).toString(), "{\"reset_task_state\":{\"task_state\":null}}");
        Assert.assertEquals(details.get("ip"), "10.254.0.3");
        Assert.assertEquals(managed.getId(), "ss2");
        Assert.assertEquals(managed.getStatus(), "manage_starting");
        Assert.assertEquals(check.get("compatible"), Boolean.FALSE);
        Assert.assertEquals(progress.get("destination_share_server_id"), "ss3");
    }

    public void shareNetworkSecurityServices() throws Exception {
        respondWith(202);
        respondWith(200, "{\"compatible\": true, \"requested_operation\": {\"operation\": \"update_security_service\"}}");
        respondWith(200, "{\"compatible\": null, \"hosts_check_result\": {}}");
        respondWith(202);

        var ext = osv3().share().shareServersExt();
        Assert.assertTrue(ext.updateSecurityService("n1", "ss-old", "ss-new").isSuccess());
        Map<String, Object> update = ext.checkUpdateSecurityService("n1", "ss-old", "ss-new", false);
        ext.checkAddSecurityService("n1", "ss-new", true);
        Assert.assertTrue(ext.resetShareNetworkStatus("n1", "active").isSuccess());

        RecordedRequest first = expect("POST", P + "/share-networks/n1/action");
        Assert.assertEquals(body(first).toString(), "{\"update_security_service\":{\"current_service_id\":\"ss-old\",\"new_service_id\":\"ss-new\"}}");
        Assert.assertEquals(first.getHeader("X-OpenStack-Manila-API-Version"), "2.63");
        Assert.assertEquals(body(expect("POST", P + "/share-networks/n1/action")).toString(),
                "{\"update_security_service_check\":{\"current_service_id\":\"ss-old\",\"new_service_id\":\"ss-new\",\"reset_operation\":false}}");
        Assert.assertEquals(body(expect("POST", P + "/share-networks/n1/action")).toString(),
                "{\"add_security_service_check\":{\"security_service_id\":\"ss-new\",\"reset_operation\":true}}");
        Assert.assertEquals(body(expect("POST", P + "/share-networks/n1/action")).toString(), "{\"reset_status\":{\"status\":\"active\"}}");
        Assert.assertEquals(update.get("compatible"), Boolean.TRUE);
    }
}
