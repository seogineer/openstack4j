package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.network.ext.BgpPeer;
import org.openstack4j.model.network.ext.BgpSpeaker;
import org.openstack4j.model.network.ext.Bgpvpn;
import org.openstack4j.model.network.ext.BgpvpnPortAssociation;
import org.openstack4j.model.network.ext.BgpvpnRouterAssociation;
import org.openstack4j.model.network.options.BgpPeerOptions;
import org.openstack4j.model.network.options.BgpSpeakerOptions;
import org.openstack4j.model.network.options.BgpvpnOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext/BgpBgpvpn")
public class BgpBgpvpnTests extends AbstractNetworkingExtTest {

    private static final String SPEAKER = "{\"peers\": [], \"name\": \"bgp-speaker\", \"local_as\": 1000, \"advertise_tenant_networks\": true, \"networks\": [],"
            + " \"ip_version\": 4, \"advertise_floating_ip_host_routes\": true, \"id\": \"sp1\"}";

    public void bgpSpeakersAndPeers() throws Exception {
        respondWith(201, "{\"bgp_speaker\": " + SPEAKER + "}");
        respondWith(201, "{\"bgp_peer\": {\"auth_type\": \"none\", \"remote_as\": \"1001\", \"name\": \"bgp-peer\", \"peer_ip\": \"10.0.0.3\", \"id\": \"pe1\"}}");
        respondWith(200, "{\"bgp_peer_id\": \"pe1\"}");
        respondWith(200, "{\"network_id\": \"n1\"}");
        respondWith(200, "{\"advertised_routes\": [{\"cidr\": \"192.168.10.0/24\", \"nexthop\": \"10.0.0.1\"}]}");
        respondWith(200, "{\"agents\": [{\"id\": \"a1\", \"binary\": \"neutron-bgp-dragent\", \"alive\": true}]}");
        respondWith(201);
        respondWith(200, "{\"bgp_speakers\": [" + SPEAKER + "]}");
        respondWith(204);
        respondWith(200, "{\"bgp_peer_id\": \"pe1\"}");

        var speakers = osv3().networking().bgpSpeakers();
        BgpSpeaker speaker = speakers.create(BgpSpeakerOptions.create("bgp-speaker", 1000L, 4));
        BgpPeer peer = osv3().networking().bgpPeers().create(BgpPeerOptions.create("bgp-peer", "10.0.0.3", 1001L, "none"));
        Assert.assertTrue(speakers.addPeer("sp1", "pe1").isSuccess());
        Assert.assertTrue(speakers.addGatewayNetwork("sp1", "n1").isSuccess());
        List<Map<String, Object>> routes = speakers.advertisedRoutes("sp1");
        List<Map<String, Object>> agents = speakers.dragents("sp1");
        Assert.assertTrue(speakers.addToDragent("a1", "sp1").isSuccess());
        List<? extends BgpSpeaker> hosted = speakers.listOnDragent("a1");
        Assert.assertTrue(speakers.removeFromDragent("a1", "sp1").isSuccess());
        Assert.assertTrue(speakers.removePeer("sp1", "pe1").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v2.0/bgp-speakers")).toString(), "{\"bgp_speaker\":{\"name\":\"bgp-speaker\",\"local_as\":1000,\"ip_version\":4}}");
        Assert.assertEquals(body(expect("POST", "/v2.0/bgp-peers")).toString(), "{\"bgp_peer\":{\"name\":\"bgp-peer\",\"peer_ip\":\"10.0.0.3\",\"remote_as\":1001,\"auth_type\":\"none\"}}");
        Assert.assertEquals(body(expect("PUT", "/v2.0/bgp-speakers/sp1/add_bgp_peer")).toString(), "{\"bgp_peer_id\":\"pe1\"}");
        Assert.assertEquals(body(expect("PUT", "/v2.0/bgp-speakers/sp1/add_gateway_network")).toString(), "{\"network_id\":\"n1\"}");
        expect("GET", "/v2.0/bgp-speakers/sp1/get_advertised_routes");
        expect("GET", "/v2.0/bgp-speakers/sp1/bgp-dragents");
        Assert.assertEquals(body(expect("POST", "/v2.0/agents/a1/bgp-drinstances")).toString(), "{\"bgp_speaker_id\":\"sp1\"}");
        expect("GET", "/v2.0/agents/a1/bgp-drinstances");
        expect("DELETE", "/v2.0/agents/a1/bgp-drinstances/sp1");
        expect("PUT", "/v2.0/bgp-speakers/sp1/remove_bgp_peer");
        Assert.assertEquals(speaker.getLocalAs(), Long.valueOf(1000));
        Assert.assertEquals(peer.getRemoteAs(), Long.valueOf(1001));
        Assert.assertEquals(routes.get(0).get("nexthop"), "10.0.0.1");
        Assert.assertEquals(agents.get(0).get("alive"), Boolean.TRUE);
        Assert.assertEquals(hosted.get(0).isAdvertiseTenantNetworks(), Boolean.TRUE);
    }

    public void bgpvpnsAndAssociations() throws Exception {
        respondWith(201, "{\"bgpvpn\": {\"id\": \"vpn1\", \"type\": \"l3\", \"route_targets\": [\"64512:1444\"], \"vni\": 1000, \"networks\": []}}");
        respondWith(201, "{\"network_association\": {\"id\": \"na1\", \"network_id\": \"n1\"}}");
        respondWith(201, "{\"router_association\": {\"id\": \"ra1\", \"router_id\": \"r1\", \"advertise_extra_routes\": true}}");
        respondWith(200, "{\"router_association\": {\"id\": \"ra1\", \"router_id\": \"r1\", \"advertise_extra_routes\": false}}");
        respondWith(201, "{\"port_association\": {\"id\": \"pa1\", \"port_id\": \"p1\", \"routes\": [{\"type\": \"prefix\", \"prefix\": \"20.1.0.0/16\"}], \"advertise_fixed_ips\": true}}");
        respondWith(200, "{\"port_associations\": []}");
        respondWith(204);

        var networking = osv3().networking();
        Bgpvpn vpn = networking.bgpvpns().create(BgpvpnOptions.create().type("l3").routeTargets(List.of("64512:1444")).vni(1000));
        networking.bgpvpnAssociations().associateNetwork("vpn1", "n1");
        networking.bgpvpnAssociations().associateRouter("vpn1", "r1", null);
        BgpvpnRouterAssociation updated = networking.bgpvpnAssociations().updateRouterAssociation("vpn1", "ra1", false);
        BgpvpnPortAssociation port = networking.bgpvpnAssociations().associatePort("vpn1",
                Map.of("port_id", "p1", "routes", List.of(Map.of("type", "prefix", "prefix", "20.1.0.0/16"))));
        networking.bgpvpnAssociations().listPortAssociations("vpn1");
        Assert.assertTrue(networking.bgpvpnAssociations().deleteNetworkAssociation("vpn1", "na1").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v2.0/bgpvpn/bgpvpns")).toString(), "{\"bgpvpn\":{\"type\":\"l3\",\"route_targets\":[\"64512:1444\"],\"vni\":1000}}");
        Assert.assertEquals(body(expect("POST", "/v2.0/bgpvpn/bgpvpns/vpn1/network_associations")).toString(), "{\"network_association\":{\"network_id\":\"n1\"}}");
        Assert.assertEquals(body(expect("POST", "/v2.0/bgpvpn/bgpvpns/vpn1/router_associations")).toString(), "{\"router_association\":{\"router_id\":\"r1\"}}");
        Assert.assertEquals(body(expect("PUT", "/v2.0/bgpvpn/bgpvpns/vpn1/router_associations/ra1")).toString(), "{\"router_association\":{\"advertise_extra_routes\":false}}");
        Assert.assertEquals(body(expect("POST", "/v2.0/bgpvpn/bgpvpns/vpn1/port_associations")).get("port_association").get("port_id").asText(), "p1");
        expect("GET", "/v2.0/bgpvpn/bgpvpns/vpn1/port_associations");
        expect("DELETE", "/v2.0/bgpvpn/bgpvpns/vpn1/network_associations/na1");
        Assert.assertEquals(vpn.getVni(), Integer.valueOf(1000));
        Assert.assertEquals(updated.isAdvertiseExtraRoutes(), Boolean.FALSE);
        Assert.assertEquals(port.getRoutes().get(0).get("prefix"), "20.1.0.0/16");
    }

    public void fourByteAsNumbersAndLocalPref() throws Exception {
        respondWith(200, "{\"bgp_speaker\": {\"id\": \"sp2\", \"local_as\": 4200000000, \"ip_version\": 4}}");
        respondWith(201, "{\"bgp_peer\": {\"id\": \"pe2\", \"remote_as\": 4294967294, \"peer_ip\": \"10.0.0.4\", \"auth_type\": \"none\"}}");
        respondWith(200, "{\"bgpvpns\": [{\"id\": \"v2\", \"local_pref\": 4294967295}]}");

        BgpSpeaker speaker = osv3().networking().bgpSpeakers().get("sp2");
        BgpPeer peer = osv3().networking().bgpPeers().create(BgpPeerOptions.create("p", "10.0.0.4", 4294967294L, "none"));
        List<? extends Bgpvpn> vpns = osv3().networking().bgpvpns().list();

        takeRequest();
        Assert.assertEquals(body(takeRequest()).get("bgp_peer").get("remote_as").asLong(), 4294967294L);
        takeRequest();
        Assert.assertEquals(speaker.getLocalAs(), Long.valueOf(4200000000L));
        Assert.assertEquals(peer.getRemoteAs(), Long.valueOf(4294967294L));
        Assert.assertEquals(vpns.get(0).getLocalPref(), Long.valueOf(4294967295L));
    }
}
