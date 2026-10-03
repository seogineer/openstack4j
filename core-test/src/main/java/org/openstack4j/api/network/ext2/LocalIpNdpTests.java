package org.openstack4j.api.network.ext2;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.LocalIp;
import org.openstack4j.model.network.ext.LocalIpPortAssociation;
import org.openstack4j.model.network.ext.NdpProxy;
import org.openstack4j.model.network.options.LocalIpOptions;
import org.openstack4j.model.network.options.NdpProxyOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/LocalIpsNdp")
public class LocalIpNdpTests extends AbstractNetworkingExtTest {

    private static final String LIP = "d23abc8d-2991-4a55-ba98-2aaea84cc72f";

    public void localIps() throws Exception {
        String lip = "{\"id\": \"" + LIP + "\", \"name\": \"test_local_ip\", \"description\": \"local ip for testing\", \"project_id\": \"" + PROJECT + "\","
                + " \"local_port_id\": \"2f245a7b-796b-4f26-9cf9-9e82d248fda7\", \"network_id\": \"ce705c24-c1ef-408a-bda3-7bbd946164ab\", \"local_ip_address\": \"172.24.4.228\","
                + " \"ip_mode\": \"translate\", \"revision_number\": 1}";
        String assoc = "{\"local_ip_id\": \"" + LIP + "\", \"local_ip_address\": \"172.24.4.228\", \"fixed_port_id\": \"fp1\", \"fixed_ip\": \"10.0.0.5\", \"host\": \"host1\"}";
        respondWith(201, "{\"local_ip\": " + lip + "}");
        respondWith(200, "{\"local_ips\": [" + lip + "]}");
        respondWith(200, "{\"local_ip\": " + lip + "}");
        respondWith(200, "{\"local_ip\": " + lip + "}");
        respondWith(201, "{\"port_association\": " + assoc + "}");
        respondWith(200, "{\"port_associations\": [" + assoc + "]}");
        respondWith(204);
        respondWith(204);

        var lips = osv3().networking().localIps();
        LocalIp created = lips.create(LocalIpOptions.create().name("test_local_ip").networkId("ce705c24-c1ef-408a-bda3-7bbd946164ab"));
        lips.list();
        lips.get(LIP);
        lips.update(LIP, LocalIpOptions.update().description("changed"));
        LocalIpPortAssociation association = lips.associatePort(LIP, "fp1", "10.0.0.5");
        List<? extends LocalIpPortAssociation> associations = lips.portAssociations(LIP);
        lips.disassociatePort(LIP, "fp1");
        lips.delete(LIP);

        RecordedRequest create = expect("POST", "/v2.0/local_ips");
        Assert.assertEquals(body(create).get("local_ip").get("network_id").asText(), "ce705c24-c1ef-408a-bda3-7bbd946164ab");
        expect("GET", "/v2.0/local_ips");
        expect("GET", "/v2.0/local_ips/" + LIP);
        expect("PUT", "/v2.0/local_ips/" + LIP);
        RecordedRequest associate = expect("POST", "/v2.0/local_ips/" + LIP + "/port_associations");
        Assert.assertEquals(body(associate).get("port_association").get("fixed_ip").asText(), "10.0.0.5");
        expect("GET", "/v2.0/local_ips/" + LIP + "/port_associations");
        expect("DELETE", "/v2.0/local_ips/" + LIP + "/port_associations/fp1");
        expect("DELETE", "/v2.0/local_ips/" + LIP);
        Assert.assertEquals(created.getIpMode(), "translate");
        Assert.assertEquals(association.getHost(), "host1");
        Assert.assertEquals(associations.get(0).getFixedPortId(), "fp1");
    }

    public void associatePortWithoutFixedIpOmitsIt() throws Exception {
        respondWith(201, "{\"port_association\": {\"local_ip_id\": \"" + LIP + "\", \"fixed_port_id\": \"fp1\"}}");
        osv3().networking().localIps().associatePort(LIP, "fp1", null);
        Assert.assertFalse(body(takeRequest()).get("port_association").has("fixed_ip"));
    }

    public void ndpProxies() throws Exception {
        String proxy = "{\"name\": \"proxy1\", \"id\": \"np1\", \"router_id\": \"r1\", \"port_id\": \"p1\", \"ip_address\": \"2001::1:56\", \"revision_number\": 1,"
                + " \"project_id\": \"" + PROJECT + "\", \"description\": \"\"}";
        respondWith(201, "{\"ndp_proxy\": " + proxy + "}");
        respondWith(200, "{\"ndp_proxies\": [" + proxy + "]}");
        respondWith(200, "{\"ndp_proxy\": " + proxy + "}");
        respondWith(200, "{\"ndp_proxy\": " + proxy + "}");
        respondWith(204);

        var proxies = osv3().networking().ndpProxies();
        NdpProxy created = proxies.create(NdpProxyOptions.create("r1", "p1").name("proxy1"));
        proxies.list();
        proxies.get("np1");
        proxies.update("np1", NdpProxyOptions.update().description("changed"));
        proxies.delete("np1");

        RecordedRequest create = expect("POST", "/v2.0/ndp_proxies");
        Assert.assertEquals(body(create).get("ndp_proxy").get("router_id").asText(), "r1");
        expect("GET", "/v2.0/ndp_proxies");
        expect("GET", "/v2.0/ndp_proxies/np1");
        expect("PUT", "/v2.0/ndp_proxies/np1");
        expect("DELETE", "/v2.0/ndp_proxies/np1");
        Assert.assertEquals(created.getIpAddress(), "2001::1:56");
    }
}
