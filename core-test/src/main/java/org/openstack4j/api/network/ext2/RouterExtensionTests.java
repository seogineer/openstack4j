package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.ext.ConntrackHelper;
import org.openstack4j.model.network.options.ConntrackHelperOptions;
import org.openstack4j.openstack.networking.domain.NeutronHostRoute;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/RouterExtensions")
public class RouterExtensionTests extends AbstractNetworkingExtTest {

    private static final String R = "/v2.0/routers/r1";

    public void extraRoutes() throws Exception {
        respondWith(200, "{\"router\": {\"id\": \"r1\", \"name\": \"router1\", \"routes\": [{\"destination\": \"10.0.3.0/24\", \"nexthop\": \"10.0.0.13\"}]}}");
        respondWith(200, "{\"router\": {\"id\": \"r1\", \"name\": \"router1\", \"routes\": []}}");

        Router added = osv3().networking().router().addExtraRoutes("r1", List.of(new NeutronHostRoute("10.0.3.0/24", "10.0.0.13")));
        Router removed = osv3().networking().router().removeExtraRoutes("r1", List.of(new NeutronHostRoute("10.0.3.0/24", "10.0.0.13")));

        RecordedRequest add = expect("PUT", R + "/add_extraroutes");
        Assert.assertEquals(body(add).get("router").get("routes").get(0).get("nexthop").asText(), "10.0.0.13");
        Assert.assertEquals(body(add).get("router").size(), 1);
        expect("PUT", R + "/remove_extraroutes");
        Assert.assertEquals(added.getRoutes().get(0).getDestination(), "10.0.3.0/24");
        Assert.assertTrue(removed.getRoutes().isEmpty());
    }

    public void externalGateways() throws Exception {
        String router = "{\"router\": {\"id\": \"r1\", \"name\": \"router1\", \"external_gateway_info\": {\"enable_snat\": false, \"external_fixed_ips\": [], \"network_id\": \"n1\"},"
                + " \"external_gateways\": [{\"enable_snat\": false, \"external_fixed_ips\": [{\"ip_address\": \"192.0.2.2\", \"subnet_id\": \"s1\"}], \"network_id\": \"n1\"}]}}";
        respondWith(200, router);
        respondWith(200, router);
        respondWith(200, router);

        List<Map<String, Object>> gateways = List.of(Map.of("network_id", "n1", "enable_snat", false,
                "external_fixed_ips", List.of(Map.of("ip_address", "192.0.2.2", "subnet_id", "s1"))));
        Router added = osv3().networking().router().addExternalGateways("r1", gateways);
        osv3().networking().router().updateExternalGateways("r1", gateways);
        osv3().networking().router().removeExternalGateways("r1", List.of(Map.of("network_id", "n1")));

        RecordedRequest add = expect("PUT", R + "/add_external_gateways");
        Assert.assertEquals(body(add).get("router").get("external_gateways").get(0).get("external_fixed_ips").get(0).get("subnet_id").asText(), "s1");
        expect("PUT", R + "/update_external_gateways");
        expect("PUT", R + "/remove_external_gateways");
        Assert.assertEquals(added.getExternalGateways().get(0).get("network_id"), "n1");
    }

    public void conntrackHelpers() throws Exception {
        String helper = "{\"protocol\": \"tcp\", \"id\": \"ch1\", \"helper\": \"ftp\", \"port\": 21}";
        respondWith(201, "{\"conntrack_helper\": " + helper + "}");
        respondWith(200, "{\"conntrack_helpers\": [" + helper + "]}");
        respondWith(200, "{\"conntrack_helper\": " + helper + "}");
        respondWith(200, "{\"conntrack_helper\": {\"protocol\": \"tcp\", \"id\": \"ch1\", \"helper\": \"ftp\", \"port\": 2121}}");
        respondWith(204);

        var routers = osv3().networking().router();
        ConntrackHelper created = routers.createConntrackHelper("r1", ConntrackHelperOptions.create("tcp", 21, "ftp"));
        List<? extends ConntrackHelper> all = routers.listConntrackHelpers("r1");
        routers.getConntrackHelper("r1", "ch1");
        ConntrackHelper updated = routers.updateConntrackHelper("r1", "ch1", ConntrackHelperOptions.update().port(2121));
        routers.deleteConntrackHelper("r1", "ch1");

        RecordedRequest create = expect("POST", R + "/conntrack_helpers");
        Assert.assertEquals(body(create).get("conntrack_helper").get("helper").asText(), "ftp");
        expect("GET", R + "/conntrack_helpers");
        expect("GET", R + "/conntrack_helpers/ch1");
        Assert.assertEquals(body(expect("PUT", R + "/conntrack_helpers/ch1")).get("conntrack_helper").size(), 1);
        expect("DELETE", R + "/conntrack_helpers/ch1");
        Assert.assertEquals(created.getPort(), Integer.valueOf(21));
        Assert.assertEquals(all.size(), 1);
        Assert.assertEquals(updated.getPort(), Integer.valueOf(2121));
    }

    public void l3Agents() throws Exception {
        respondWith(200, "{\"agents\": [{\"id\": \"a1\", \"agent_type\": \"L3 agent\", \"host\": \"net1\", \"alive\": true, \"admin_state_up\": true, \"binary\": \"neutron-l3-agent\"}]}");
        Assert.assertEquals(osv3().networking().router().listL3Agents("r1").get(0).getId(), "a1");
        expect("GET", R + "/l3-agents");
    }

    public void existingRouterCreateBodyUnchanged() throws Exception {
        respondWith(201, "{\"router\": {\"id\": \"r1\", \"name\": \"router1\", \"admin_state_up\": true}}");
        osv3().networking().router().create(Builders.router().name("router1").adminStateUp(true).build());
        RecordedRequest create = expect("POST", "/v2.0/routers");
        Assert.assertEquals(create.getBody().readUtf8(), "{\n  \"router\" : {\n    \"name\" : \"router1\",\n    \"admin_state_up\" : true\n  }\n}");
    }
}
