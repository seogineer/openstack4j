package org.openstack4j.api.network.ext2;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.network.ext.AutoAllocatedTopology;
import org.openstack4j.model.network.ext.FloatingIPPool;
import org.openstack4j.model.network.ext.NeutronExtension;
import org.openstack4j.model.network.ext.QuotaDetail;
import org.openstack4j.model.network.ext.ServiceProvider;
import org.openstack4j.model.network.options.PortForwardingUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Network/Ext2/Basics")
public class NetworkingBasicsTests extends AbstractNetworkingExtTest {

    private static final String EXTENSIONS = "{\"extensions\": [{\"name\": \"Address group\", \"alias\": \"address-group\", \"description\": \"Support address group\", \"updated\": \"2020-11-16T10:00:00-00:00\", \"links\": []},"
            + " {\"name\": \"Quality of Service\", \"alias\": \"qos\", \"description\": \"The Quality of Service extension.\", \"updated\": \"2015-06-08T10:00:00-00:00\", \"links\": []}]}";

    public void extensions() throws Exception {
        respondWith(200, EXTENSIONS);
        respondWith(200, "{\"extension\": {\"name\": \"Quality of Service\", \"alias\": \"qos\", \"description\": \"The Quality of Service extension.\", \"updated\": \"2015-06-08T10:00:00-00:00\", \"links\": []}}");
        respondWith(200, EXTENSIONS);
        respondWith(200, EXTENSIONS);

        List<? extends NeutronExtension> all = osv3().networking().extensions().list();
        NeutronExtension qos = osv3().networking().extensions().get("qos");
        boolean hasGroups = osv3().networking().extensions().isEnabled("address-group");
        boolean hasLogging = osv3().networking().extensions().isEnabled("logging");

        expect("GET", "/v2.0/extensions");
        expect("GET", "/v2.0/extensions/qos");
        expect("GET", "/v2.0/extensions");
        expect("GET", "/v2.0/extensions");
        Assert.assertEquals(all.size(), 2);
        Assert.assertEquals(all.get(0).getAlias(), "address-group");
        Assert.assertEquals(qos.getName(), "Quality of Service");
        Assert.assertEquals(qos.getUpdated(), "2015-06-08T10:00:00-00:00");
        Assert.assertTrue(hasGroups);
        Assert.assertFalse(hasLogging);
    }

    public void serviceProvidersAndFloatingIpPools() throws Exception {
        respondWith(200, "{\"service_providers\": [{\"service_type\": \"L3_ROUTER_NAT\", \"name\": \"ovn\", \"default\": true}]}");
        respondWith(200, "{\"floatingip_pools\":[{\"subnet_id\":\"3260d5f7-ec44-44fa-821e-e9df107ce5a3\",\"subnet_name\":\"external-subnet\","
                + "\"tenant_id\":\"" + PROJECT + "\",\"network_id\":\"8e5c1b8c-0d0b-4d6b-9a52-6f8b2d6f2e3a\",\"project_id\":\"" + PROJECT + "\"}]}");

        List<? extends ServiceProvider> providers = osv3().networking().serviceProviders().list();
        List<? extends FloatingIPPool> pools = osv3().networking().floatingip().listPools();

        expect("GET", "/v2.0/service-providers");
        expect("GET", "/v2.0/floatingip_pools");
        Assert.assertEquals(providers.get(0).getServiceType(), "L3_ROUTER_NAT");
        Assert.assertTrue(providers.get(0).isDefault());
        Assert.assertEquals(pools.get(0).getSubnetName(), "external-subnet");
        Assert.assertEquals(pools.get(0).getProjectId(), PROJECT);
    }

    public void autoAllocatedTopology() throws Exception {
        respondWith(200, "{\"auto_allocated_topology\": {\"id\": \"31483d41-5c2b-481c-beef-ab501bd2e0da\", \"tenant_id\": \"" + PROJECT + "\", \"project_id\": \"" + PROJECT + "\"}}");
        respondWith(200, "{\"auto_allocated_topology\": {\"dry-run\": \"pass\"}}");
        respondWith(204);

        AutoAllocatedTopology topology = osv3().networking().autoAllocatedTopology().get(PROJECT);
        AutoAllocatedTopology check = osv3().networking().autoAllocatedTopology().validate(PROJECT);
        boolean deleted = osv3().networking().autoAllocatedTopology().delete(PROJECT).isSuccess();

        expect("GET", "/v2.0/auto-allocated-topology/" + PROJECT);
        expect("GET", "/v2.0/auto-allocated-topology/" + PROJECT + "?fields=dry-run");
        expect("DELETE", "/v2.0/auto-allocated-topology/" + PROJECT);
        Assert.assertEquals(topology.getId(), "31483d41-5c2b-481c-beef-ab501bd2e0da");
        Assert.assertEquals(topology.getProjectId(), PROJECT);
        Assert.assertEquals(check.getDryRun(), "pass");
        Assert.assertTrue(deleted);
    }

    public void autoAllocatedTopologyErrorIsRaised() throws Exception {
        respondWith(400, "{\"NeutronError\": {\"type\": \"AutoAllocationFailure\", \"message\": \"Deployment error: No default router:external network.\", \"detail\": \"\"}}");
        try {
            osv3().networking().autoAllocatedTopology().validate(PROJECT);
            Assert.fail("expected the Neutron error to surface");
        } catch (RuntimeException expected) {
            Assert.assertTrue(String.valueOf(expected.getMessage()).contains("No default router:external network"), expected.getMessage());
        }
        takeRequest();
    }

    public void quotaDefaultAndDetails() throws Exception {
        respondWith(200, "{\"quota\": {\"network\": 100, \"subnet\": 100, \"subnetpool\": -1, \"port\": 500, \"router\": 10, \"floatingip\": 50, \"rbac_policy\": 10, \"security_group\": 10, \"security_group_rule\": 100}}");
        respondWith(200, "{\"quota\": {\"network\": {\"limit\": 100, \"used\": 3, \"reserved\": 0}, \"subnetpool\": {\"limit\": -1, \"used\": 0, \"reserved\": 0}}}");

        org.openstack4j.model.network.NetQuota defaults = osv3().networking().quotas().getDefault(PROJECT);
        Map<String, ? extends QuotaDetail> details = osv3().networking().quotas().getDetails(PROJECT);

        expect("GET", "/v2.0/quotas/" + PROJECT + "/default");
        expect("GET", "/v2.0/quotas/" + PROJECT + "/details.json");
        Assert.assertEquals(defaults.getNetwork(), 100);
        Assert.assertEquals(details.get("network").getUsed(), Integer.valueOf(3));
        Assert.assertEquals(details.get("subnetpool").getLimit(), Integer.valueOf(-1));
    }

    public void portForwardingUpdate() throws Exception {
        respondWith(200, "{\"port_forwarding\": {\"id\": \"pf1\", \"protocol\": \"tcp\", \"internal_ip_address\": \"10.0.0.11\", \"internal_port\": 25,"
                + " \"internal_port_id\": \"1238be08-a2a8-4b8d-addf-fb5e2250e480\", \"external_port\": 2230, \"description\": \"changed\"}}");

        osv3().networking().floatingip().portForwarding().update("fip1", "pf1", PortForwardingUpdate.create().internalPort(25).description("changed"));

        RecordedRequest request = expect("PUT", "/v2.0/floatingips/fip1/port_forwardings/pf1");
        Assert.assertEquals(body(request).get("port_forwarding").get("internal_port").asInt(), 25);
        Assert.assertEquals(body(request).get("port_forwarding").get("description").asText(), "changed");
        Assert.assertEquals(body(request).get("port_forwarding").size(), 2);
    }
}
