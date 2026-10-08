package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.IkePolicy;
import org.openstack4j.model.network.ext.IpsecSiteConnection;
import org.openstack4j.model.network.ext.VpnEndpointGroup;
import org.openstack4j.model.network.ext.VpnService;
import org.openstack4j.model.network.options.IkePolicyOptions;
import org.openstack4j.model.network.options.IpsecPolicyOptions;
import org.openstack4j.model.network.options.IpsecSiteConnectionOptions;
import org.openstack4j.model.network.options.VpnEndpointGroupOptions;
import org.openstack4j.model.network.options.VpnServiceOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext/Vpnaas")
public class VpnaasTests extends AbstractNetworkingExtTest {

    private static final String SERVICE = "{\"router_id\": \"r1\", \"status\": \"PENDING_CREATE\", \"name\": \"myservice\", \"external_v4_ip\": \"172.32.1.11\","
            + " \"admin_state_up\": true, \"subnet_id\": null, \"id\": \"vs1\", \"flavor_id\": null}";
    private static final String CONNECTION = "{\"status\": \"PENDING_CREATE\", \"psk\": \"secret\", \"initiator\": \"bi-directional\", \"name\": \"vpnconnection1\","
            + " \"peer_cidrs\": [], \"mtu\": 1500, \"peer_ep_group_id\": \"peg\", \"local_ep_group_id\": \"leg\", \"ikepolicy_id\": \"ike1\", \"ipsecpolicy_id\": \"ipsec1\","
            + " \"vpnservice_id\": \"vs1\", \"dpd\": {\"action\": \"hold\", \"interval\": 30, \"timeout\": 120}, \"route_mode\": \"static\", \"peer_address\": \"172.24.4.233\","
            + " \"peer_id\": \"172.24.4.233\", \"id\": \"c1\", \"auth_mode\": \"psk\"}";

    public void vpnServices() throws Exception {
        respondWith(201, "{\"vpnservice\": " + SERVICE + "}");
        respondWith(200, "{\"vpnservices\": [" + SERVICE + "]}");
        respondWith(200, "{\"vpnservice\": " + SERVICE + "}");
        respondWith(200, "{\"vpnservice\": " + SERVICE + "}");
        respondWith(204);

        var services = osv3().networking().vpnServices();
        VpnService created = services.create(VpnServiceOptions.create("r1").name("myservice"));
        List<? extends VpnService> all = services.list(Map.of("router_id", "r1"));
        services.get("vs1");
        services.update("vs1", VpnServiceOptions.update().description("d"));
        Assert.assertTrue(services.delete("vs1").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v2.0/vpn/vpnservices")).toString(), "{\"vpnservice\":{\"router_id\":\"r1\",\"name\":\"myservice\"}}");
        expect("GET", "/v2.0/vpn/vpnservices?router_id=r1");
        expect("GET", "/v2.0/vpn/vpnservices/vs1");
        Assert.assertEquals(body(expect("PUT", "/v2.0/vpn/vpnservices/vs1")).toString(), "{\"vpnservice\":{\"description\":\"d\"}}");
        expect("DELETE", "/v2.0/vpn/vpnservices/vs1");
        Assert.assertEquals(created.getExternalV4Ip(), "172.32.1.11");
        Assert.assertEquals(all.get(0).isAdminStateUp(), Boolean.TRUE);
    }

    public void policiesConnectionsAndEndpointGroups() throws Exception {
        respondWith(201, "{\"ikepolicy\": {\"id\": \"ike1\", \"name\": \"ikepolicy1\", \"ike_version\": \"v2\", \"lifetime\": {\"units\": \"seconds\", \"value\": 7200}}}");
        respondWith(201, "{\"ipsecpolicy\": {\"id\": \"ipsec1\", \"name\": \"ipsecpolicy1\", \"transform_protocol\": \"esp\"}}");
        respondWith(201, "{\"endpoint_group\": {\"id\": \"peg\", \"name\": \"peers\", \"type\": \"cidr\", \"endpoints\": [\"10.2.0.0/24\"]}}");
        respondWith(201, "{\"ipsec_site_connection\": " + CONNECTION + "}");
        respondWith(200, "{\"ipsec_site_connections\": [" + CONNECTION + "]}");
        respondWith(204);

        var networking = osv3().networking();
        IkePolicy ike = networking.ikePolicies().create(IkePolicyOptions.create("ikepolicy1").ikeVersion("v2").lifetime(Map.of("units", "seconds", "value", 7200)));
        networking.ipsecPolicies().create(IpsecPolicyOptions.create("ipsecpolicy1").transformProtocol("esp"));
        VpnEndpointGroup group = networking.vpnEndpointGroups().create(VpnEndpointGroupOptions.create("cidr", List.of("10.2.0.0/24")).name("peers"));
        IpsecSiteConnection connection = networking.ipsecSiteConnections().create(
                IpsecSiteConnectionOptions.create("vs1", "ike1", "ipsec1", "172.24.4.233", "172.24.4.233", "secret").localEpGroupId("leg").peerEpGroupId("peg"));
        List<? extends IpsecSiteConnection> connections = networking.ipsecSiteConnections().list(Map.of("vpnservice_id", "vs1"));
        Assert.assertTrue(networking.ipsecSiteConnections().delete("c1").isSuccess());

        var ikeBody = body(expect("POST", "/v2.0/vpn/ikepolicies")).get("ikepolicy");
        Assert.assertEquals(ikeBody.get("ike_version").asText(), "v2");
        Assert.assertEquals(ikeBody.get("lifetime").get("value").asInt(), 7200);
        expect("POST", "/v2.0/vpn/ipsecpolicies");
        Assert.assertEquals(body(expect("POST", "/v2.0/vpn/endpoint-groups")).toString(),
                "{\"endpoint_group\":{\"type\":\"cidr\",\"endpoints\":[\"10.2.0.0/24\"],\"name\":\"peers\"}}");
        RecordedRequest create = expect("POST", "/v2.0/vpn/ipsec-site-connections");
        Assert.assertEquals(body(create).get("ipsec_site_connection").get("psk").asText(), "secret");
        Assert.assertEquals(body(create).get("ipsec_site_connection").get("peer_ep_group_id").asText(), "peg");
        expect("GET", "/v2.0/vpn/ipsec-site-connections?vpnservice_id=vs1");
        expect("DELETE", "/v2.0/vpn/ipsec-site-connections/c1");
        Assert.assertEquals(ike.getIkeVersion(), "v2");
        Assert.assertEquals(group.getEndpoints(), List.of("10.2.0.0/24"));
        Assert.assertEquals(connection.getDpd().get("interval"), 30);
        Assert.assertEquals(connections.get(0).getMtu(), Integer.valueOf(1500));
    }

    public void missingVpnServiceIsNullAndListRaises() throws Exception {
        respondWith(404, "{\"NeutronError\": {\"type\": \"VPNServiceNotFound\", \"message\": \"VPNService vs9 could not be found\", \"detail\": \"\"}}");
        respondWith(404, "{\"NeutronError\": {\"type\": \"HTTPNotFound\", \"message\": \"The resource could not be found.\", \"detail\": \"\"}}");
        Assert.assertNull(osv3().networking().vpnServices().get("vs9"));
        takeRequest();
        try {
            osv3().networking().ikePolicies().list();
            Assert.fail("expected a ResponseException");
        } catch (org.openstack4j.api.exceptions.ResponseException e) {
            Assert.assertEquals(e.getStatus(), 404);
        } finally {
            takeRequest();
        }
    }
}
