package org.openstack4j.api.network;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.api.network.ext2.AbstractNetworkingExtTest;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.network.Agent;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.openstack.networking.domain.NeutronAgent;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronRouter;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Neutron response-only fields: read from the response, never sent back on create/update. */
@Test(suiteName = "Network/ResponseFields")
public class NeutronResponseFieldTests extends AbstractNetworkingExtTest {

    private static final String PORT = "{\"port\":{\"id\":\"p1\",\"network_id\":\"n1\",\"name\":\"port\","
            + "\"qos_policy_id\":\"q1\",\"qos_network_policy_id\":\"q2\"}}";
    private static final String ROUTER = "{\"router\":{\"id\":\"r1\",\"name\":\"router\",\"project_id\":\"p1\",\"tenant_id\":\"p1\"}}";

    public void networkQosPolicyId() throws Exception {
        respondWith(200, "{\"network\":{\"id\":\"n1\",\"name\":\"net\",\"qos_policy_id\":\"q1\"}}");
        Network network = osv3().networking().network().get("n1");
        expect("GET", "/networks/n1");
        Assert.assertEquals(network.getQosPolicyId(), "q1");
    }

    public void networkWithoutQosPolicyIdIsNull() throws Exception {
        respondWith(200, "{\"network\":{\"id\":\"n1\",\"name\":\"net\"}}");
        Network network = osv3().networking().network().get("n1");
        expect("GET", "/networks/n1");
        Assert.assertNull(network.getQosPolicyId());
    }

    public void portQosFieldsNotSentOnUpdate() throws Exception {
        respondWith(200, PORT);
        respondWith(200, PORT);
        Port port = osv3().networking().port().get("p1");
        expect("GET", "/ports/p1");
        Assert.assertEquals(port.getQosPolicyId(), "q1");
        Assert.assertEquals(port.getQosNetworkPolicyId(), "q2");

        osv3().networking().port().update(port);
        JsonNode sent = body(expect("PUT", "/ports/p1")).get("port");
        Assert.assertFalse(sent.has("qos_policy_id"), sent.toString());
        Assert.assertFalse(sent.has("qos_network_policy_id"), sent.toString());
    }

    public void routerProjectIdNotSentOnCreate() throws Exception {
        respondWith(200, ROUTER);
        respondWith(200, ROUTER);
        Router router = osv3().networking().router().get("r1");
        expect("GET", "/routers/r1");
        Assert.assertEquals(router.getProjectId(), "p1");
        Assert.assertEquals(router.getTenantId(), "p1");

        osv3().networking().router().create(router);
        JsonNode sent = body(expect("POST", "/routers")).get("router");
        Assert.assertFalse(sent.has("project_id"), sent.toString());
    }

    public void agentAvailabilityZone() throws Exception {
        respondWith(200, "{\"agents\":[{\"id\":\"a1\",\"agent_type\":\"DHCP agent\",\"availability_zone\":\"nova\"}]}");
        List<? extends Agent> agents = osv3().networking().agent().list();
        expect("GET", "/agents");
        Assert.assertEquals(agents.get(0).getAvailabilityZone(), "nova");
    }

    public void responseOnlyFieldsNotSerialized() throws Exception {
        NeutronNetwork network = ObjectMapperSingleton.getContext(NeutronNetwork.class).readValue(
                "{\"network\":{\"id\":\"n1\",\"qos_policy_id\":\"q1\"}}", NeutronNetwork.class);
        Assert.assertEquals(network.getQosPolicyId(), "q1");
        assertNotSerialized(NeutronNetwork.class, network, "qos_policy_id");

        NeutronPort port = ObjectMapperSingleton.getContext(NeutronPort.class).readValue(PORT, NeutronPort.class);
        Assert.assertEquals(port.getQosPolicyId(), "q1");
        Assert.assertEquals(port.getQosNetworkPolicyId(), "q2");
        assertNotSerialized(NeutronPort.class, port, "qos_policy_id", "qos_network_policy_id");

        NeutronRouter router = ObjectMapperSingleton.getContext(NeutronRouter.class).readValue(ROUTER, NeutronRouter.class);
        Assert.assertEquals(router.getProjectId(), "p1");
        assertNotSerialized(NeutronRouter.class, router, "project_id");

        NeutronAgent agent = ObjectMapperSingleton.getContext(NeutronAgent.class).readValue(
                "{\"agent\":{\"id\":\"a1\",\"agent_type\":\"DHCP agent\",\"availability_zone\":\"nova\"}}", NeutronAgent.class);
        Assert.assertEquals(agent.getAvailabilityZone(), "nova");
        assertNotSerialized(NeutronAgent.class, agent, "availability_zone");
    }

    private static final String ROUTER_FULL = "{\"router\":{\"id\":\"r1\",\"name\":\"router\",\"admin_state_up\":true,\"tenant_id\":\"p1\","
            + "\"description\":\"d\",\"availability_zone_hints\":[\"az1\"],\"availability_zones\":[\"az1\"],\"ha\":false,"
            + "\"flavor_id\":\"f1\",\"service_type_id\":\"s1\",\"created_at\":\"2018-03-19T19:16:56Z\",\"updated_at\":\"2018-03-19T19:16:57Z\","
            + "\"tags\":[\"t\"],\"revision_number\":3,\"conntrack_helpers\":[{\"protocol\":\"tcp\",\"port\":21,\"helper\":\"ftp\"}],"
            + "\"routes\":[{\"destination\":\"10.1.0.0/24\",\"nexthop\":\"10.0.0.9\"}],"
            + "\"external_gateway_info\":{\"network_id\":\"ext\",\"enable_snat\":true,"
            + "\"external_fixed_ips\":[{\"subnet_id\":\"s\",\"ip_address\":\"172.24.4.6\"}]}}}";
    private static final String[] ROUTER_RESPONSE_KEYS = {"description", "availability_zone_hints", "availability_zones", "ha", "flavor_id",
            "service_type_id", "created_at", "updated_at", "tags", "revision_number", "conntrack_helpers"};
    private static final String FIP_FULL = "{\"floatingip\":{\"id\":\"f1\",\"floating_network_id\":\"ext\",\"floating_ip_address\":\"172.24.4.2\","
            + "\"port_id\":\"p1\",\"dns_name\":\"host\",\"dns_domain\":\"example.org.\","
            + "\"port_details\":{\"name\":\"port\",\"network_id\":\"n1\",\"mac_address\":\"fa:16:3e:00:00:01\",\"admin_state_up\":true,"
            + "\"status\":\"ACTIVE\",\"device_id\":\"d1\",\"device_owner\":\"compute:nova\"},"
            + "\"port_forwardings\":[{\"internal_port_id\":\"p1\",\"external_port\":8080,\"protocol\":\"tcp\"}]}}";
    private static final String[] FIP_RESPONSE_KEYS = {"dns_name", "dns_domain", "port_details", "port_forwardings"};

    public void routerResponseFields() throws Exception {
        respondWith(200, ROUTER_FULL);

        Router router = osv3().networking().router().get("r1");

        expect("GET", "/routers/r1");
        Assert.assertEquals(router.getDescription(), "d");
        Assert.assertEquals(router.getAvailabilityZoneHints(), List.of("az1"));
        Assert.assertEquals(router.getAvailabilityZones(), List.of("az1"));
        Assert.assertEquals(router.getHa(), Boolean.FALSE);
        Assert.assertEquals(router.getFlavorId(), "f1");
        Assert.assertEquals(router.getServiceTypeId(), "s1");
        Assert.assertEquals(router.getCreatedAt().getTime(), 1521487016000L);
        Assert.assertEquals(router.getUpdatedAt().getTime(), 1521487017000L);
        Assert.assertEquals(router.getTags(), List.of("t"));
        Assert.assertEquals(router.getRevisionNumber(), Integer.valueOf(3));
        Assert.assertEquals(router.getConntrackHelpers().get(0).get("helper"), "ftp");
        Assert.assertEquals(router.getConntrackHelpers().get(0).get("port"), 21);
        Assert.assertEquals(router.getExternalGatewayInfo().getExternalFixedIps().size(), 1);
        Assert.assertEquals(router.getExternalGatewayInfo().getExternalFixedIps().get(0).getIpAddress(), "172.24.4.6");
        Assert.assertEquals(router.getExternalGatewayInfo().getExternalFixedIps().get(0).getSubnetId(), "s");
        Assert.assertEquals(router.getExternalGatewayInfo().getNetworkId(), "ext");
        Assert.assertEquals(router.getRoutes().size(), 1);
    }

    public void routerWithoutResponseFieldsIsNull() throws Exception {
        respondWith(200, ROUTER);

        Router router = osv3().networking().router().get("r1");

        expect("GET", "/routers/r1");
        Assert.assertNull(router.getDescription());
        Assert.assertNull(router.getAvailabilityZoneHints());
        Assert.assertNull(router.getHa());
        Assert.assertNull(router.getCreatedAt());
        Assert.assertNull(router.getTags());
        Assert.assertNull(router.getRevisionNumber());
        Assert.assertNull(router.getConntrackHelpers());
        Assert.assertNull(router.getExternalGatewayInfo());
    }

    public void routerCreateDoesNotResendResponseFields() throws Exception {
        respondWith(200, ROUTER_FULL);
        respondWith(201, ROUTER_FULL);
        Router router = osv3().networking().router().get("r1");
        expect("GET", "/routers/r1");

        osv3().networking().router().create(router);

        JsonNode sent = body(expect("POST", "/routers")).get("router");
        assertKeysAbsent(sent, ROUTER_RESPONSE_KEYS);
        assertKeysAbsent(sent, "project_id", "projectId");
        Assert.assertEquals(sent.get("external_gateway_info").get("network_id").asText(), "ext");
        assertKeysAbsent(sent.get("external_gateway_info"), "external_fixed_ips", "externalFixedIps");
    }

    public void routerUpdateGatewayWithoutFixedIps() throws Exception {
        respondWith(200, ROUTER_FULL);
        respondWith(200, ROUTER_FULL);
        Router router = osv3().networking().router().get("r1");
        expect("GET", "/routers/r1");

        osv3().networking().router().update(router);

        JsonNode sent = body(expect("PUT", "/routers/r1")).get("router");
        assertKeysAbsent(sent, ROUTER_RESPONSE_KEYS);
        java.util.Set<String> gatewayKeys = new java.util.HashSet<>();
        sent.get("external_gateway_info").fieldNames().forEachRemaining(gatewayKeys::add);
        Assert.assertEquals(gatewayKeys, java.util.Set.of("network_id", "enable_snat"));
        Assert.assertEquals(sent.get("routes").size(), 1);
    }

    public void routerBuilderCreateBodyUnchanged() throws Exception {
        respondWith(201, ROUTER_FULL);

        osv3().networking().router().create(org.openstack4j.api.Builders.router().name("r").adminStateUp(true).externalGateway("ext", true).build());

        JsonNode sent = body(expect("POST", "/routers")).get("router");
        java.util.Set<String> keys = new java.util.HashSet<>();
        sent.fieldNames().forEachRemaining(keys::add);
        Assert.assertEquals(keys, java.util.Set.of("name", "admin_state_up", "external_gateway_info"));
        java.util.Set<String> gatewayKeys = new java.util.HashSet<>();
        sent.get("external_gateway_info").fieldNames().forEachRemaining(gatewayKeys::add);
        Assert.assertEquals(gatewayKeys, java.util.Set.of("network_id", "enable_snat"));
    }

    public void floatingIpResponseFields() throws Exception {
        respondWith(200, FIP_FULL);

        NetFloatingIP fip = osv3().networking().floatingip().get("f1");

        expect("GET", "/floatingips/f1");
        Assert.assertEquals(fip.getDnsName(), "host");
        Assert.assertEquals(fip.getDnsDomain(), "example.org.");
        Assert.assertEquals(fip.getPortDetails().get("device_owner"), "compute:nova");
        Assert.assertEquals(fip.getPortDetails().get("admin_state_up"), Boolean.TRUE);
        Assert.assertEquals(fip.getPortForwardings().get(0).get("external_port"), 8080);
    }

    public void floatingIpNullPortDetailsAndMissingKeys() throws Exception {
        respondWith(200, "{\"floatingip\":{\"id\":\"f1\",\"port_details\":null}}");
        respondWith(200, "{\"floatingip\":{\"id\":\"f1\"}}");

        NetFloatingIP withNull = osv3().networking().floatingip().get("f1");
        NetFloatingIP without = osv3().networking().floatingip().get("f1");

        expect("GET", "/floatingips/f1");
        expect("GET", "/floatingips/f1");
        Assert.assertNull(withNull.getPortDetails());
        Assert.assertNull(without.getPortDetails());
        Assert.assertNull(without.getDnsName());
        Assert.assertNull(without.getPortForwardings());
    }

    public void floatingIpCreateDoesNotResendResponseFields() throws Exception {
        respondWith(200, FIP_FULL);
        respondWith(201, FIP_FULL);
        NetFloatingIP fip = osv3().networking().floatingip().get("f1");
        expect("GET", "/floatingips/f1");

        osv3().networking().floatingip().create(fip);

        JsonNode sent = body(expect("POST", "/floatingips")).get("floatingip");
        assertKeysAbsent(sent, FIP_RESPONSE_KEYS);
        Assert.assertEquals(sent.get("floating_network_id").asText(), "ext");
    }

    public void routerAndFloatingIpFieldsNotSerialized() throws Exception {
        NeutronRouter router = ObjectMapperSingleton.getContext(NeutronRouter.class).readValue(ROUTER_FULL, NeutronRouter.class);
        Assert.assertEquals(router.getHa(), Boolean.FALSE);
        assertNotSerialized(NeutronRouter.class, router, ROUTER_RESPONSE_KEYS);
        assertNotSerialized(NeutronRouter.class, router, "external_fixed_ips");

        NeutronFloatingIP fip = ObjectMapperSingleton.getContext(NeutronFloatingIP.class).readValue(FIP_FULL, NeutronFloatingIP.class);
        Assert.assertEquals(fip.getDnsName(), "host");
        assertNotSerialized(NeutronFloatingIP.class, fip, FIP_RESPONSE_KEYS);
    }

    private static final String NETWORK_DESC = "{\"network\":{\"id\":\"n1\",\"name\":\"net\",\"description\":\"net desc\",\"qos_policy_id\":\"q1\"}}";
    private static final String PORT_DESC = "{\"port\":{\"id\":\"p1\",\"network_id\":\"n1\",\"name\":\"port\",\"description\":\"port desc\"}}";
    private static final String QOS_POLICY = "{\"policy\":{\"id\":\"q1\",\"name\":\"10Mbit\",\"shared\":false,\"is_default\":false,"
            + "\"created_at\":\"2017-05-11T17:21:34Z\",\"updated_at\":\"2017-05-11T17:21:35Z\",\"rules\":[],\"tags\":[]}}";
    private static final String AVAILABILITY = "{\"network_ip_availability\":{\"network_id\":\"n1\",\"network_name\":\"private\","
            + "\"tenant_id\":\"t\",\"project_id\":\"t\",\"total_ips\":18446744073709551616,\"used_ips\":4,"
            + "\"ip_availability_details\":{\"total_ips_in_subnet\":18446744073709551616,\"total_ips_in_allocation_pool\":253,"
            + "\"used_ips_in_subnet\":4,\"used_ips_in_allocation_pool\":4},"
            + "\"subnet_ip_availability\":[{\"subnet_id\":\"s1\",\"subnet_name\":\"sub\",\"ip_version\":4,\"cidr\":\"10.0.0.0/24\","
            + "\"total_ips\":253,\"used_ips\":2,"
            + "\"ip_availability_details\":{\"total_ips_in_subnet\":256,\"total_ips_in_allocation_pool\":253,"
            + "\"used_ips_in_subnet\":3,\"used_ips_in_allocation_pool\":2}}]}}";
    private static final String AGENT = "{\"agent\":{\"id\":\"a1\",\"agent_type\":\"Open vSwitch agent\",\"host\":\"h1\","
            + "\"configurations\":{\"bridge_mappings\":{\"physnet1\":\"br-ex\"},\"devices\":3}}}";

    public void networkAndPortDescription() throws Exception {
        respondWith(200, NETWORK_DESC);
        respondWith(200, PORT_DESC);

        Network network = osv3().networking().network().get("n1");
        Port port = osv3().networking().port().get("p1");

        expect("GET", "/networks/n1");
        expect("GET", "/ports/p1");
        Assert.assertEquals(network.getDescription(), "net desc");
        Assert.assertEquals(port.getDescription(), "port desc");
    }

    public void networkAndPortDescriptionNotResent() throws Exception {
        respondWith(200, NETWORK_DESC);
        respondWith(200, PORT_DESC);
        respondWith(201, NETWORK_DESC);
        respondWith(201, PORT_DESC);
        respondWith(200, PORT_DESC);
        Network network = osv3().networking().network().get("n1");
        Port port = osv3().networking().port().get("p1");
        expect("GET", "/networks/n1");
        expect("GET", "/ports/p1");

        osv3().networking().network().create(network);
        osv3().networking().port().create(port);
        osv3().networking().port().update(port);

        JsonNode createdNetwork = body(expect("POST", "/networks")).get("network");
        assertKeysAbsent(createdNetwork, "description", "qos_policy_id");
        JsonNode createdPort = body(expect("POST", "/ports")).get("port");
        assertKeysAbsent(createdPort, "description");
        JsonNode updatedPort = body(expect("PUT", "/ports/p1")).get("port");
        assertKeysAbsent(updatedPort, "description");
    }

    public void networkWithoutDescriptionAndBuilderQosBodyUnchanged() throws Exception {
        respondWith(201, NETWORK_DESC);
        osv3().networking().network().create(org.openstack4j.api.Builders.network().name("n").qosPolicyId("q1").build());

        JsonNode sent = body(expect("POST", "/networks")).get("network");
        Assert.assertEquals(sent.get("qos_policy_id").asText(), "q1");
        assertKeysAbsent(sent, "description");
    }

    public void agentConfigurations() throws Exception {
        respondWith(200, AGENT);

        Agent agent = osv3().networking().agent().getAgent("a1");

        expect("GET", "/agents/a1");
        Assert.assertEquals(agent.getConfigurations().get("devices"), 3);
        Assert.assertEquals(((java.util.Map<?, ?>) agent.getConfigurations().get("bridge_mappings")).get("physnet1"), "br-ex");
        NeutronAgent model = ObjectMapperSingleton.getContext(NeutronAgent.class).readValue(AGENT, NeutronAgent.class);
        assertNotSerialized(NeutronAgent.class, model, "configurations");
    }

    public void qosPolicyTimestamps() throws Exception {
        respondWith(200, QOS_POLICY);
        respondWith(201, QOS_POLICY);

        org.openstack4j.model.network.ext.NetQosPolicy policy = osv3().networking().netQosPolicy().get("q1");
        expect("GET", "/qos/policies/q1");
        Assert.assertEquals(policy.getCreatedAt().getTime(), 1494523294000L);
        Assert.assertEquals(policy.getUpdatedAt().getTime(), 1494523295000L);

        osv3().networking().netQosPolicy().create(policy);
        JsonNode sent = body(expect("POST", "/qos/policies")).get("policy");
        assertKeysAbsent(sent, "created_at", "updated_at", "createdAt", "updatedAt");
        Assert.assertEquals(sent.get("name").asText(), "10Mbit");
    }

    public void ipAvailabilityDetailsAreBigIntegers() throws Exception {
        respondWith(200, AVAILABILITY);

        org.openstack4j.model.network.ext.NetworkIPAvailability availability = osv3().networking().networkIPAvailability().get("n1");

        expect("GET", "/network-ip-availabilities/n1");
        java.math.BigInteger big = new java.math.BigInteger("18446744073709551616");
        Assert.assertEquals(availability.getIpAvailabilityDetails().get("total_ips_in_subnet"), big);
        Assert.assertEquals(availability.getIpAvailabilityDetails().get("used_ips_in_allocation_pool"), java.math.BigInteger.valueOf(4));
        Assert.assertEquals(availability.getTotalIps(), big);
        org.openstack4j.model.network.ext.SubnetIPAvailability subnet = availability.getSubnetIPAvailabilities().get(0);
        Assert.assertEquals(subnet.getIpAvailabilityDetails().get("total_ips_in_subnet"), java.math.BigInteger.valueOf(256));
        Assert.assertEquals(subnet.getIpAvailabilityDetails().get("total_ips_in_allocation_pool"), java.math.BigInteger.valueOf(253));
    }

    public void ipAvailabilityWithoutExtensionIsNull() throws Exception {
        respondWith(200, "{\"network_ip_availability\":{\"network_id\":\"n1\",\"total_ips\":10,\"used_ips\":1,"
                + "\"subnet_ip_availability\":[{\"subnet_id\":\"s1\",\"total_ips\":10,\"used_ips\":1}]}}");

        org.openstack4j.model.network.ext.NetworkIPAvailability availability = osv3().networking().networkIPAvailability().get("n1");

        expect("GET", "/network-ip-availabilities/n1");
        Assert.assertNull(availability.getIpAvailabilityDetails());
        Assert.assertNull(availability.getSubnetIPAvailabilities().get(0).getIpAvailabilityDetails());
    }

    public void networkPortQosIpAvailabilityNotSerialized() throws Exception {
        NeutronNetwork network = ObjectMapperSingleton.getContext(NeutronNetwork.class).readValue(NETWORK_DESC, NeutronNetwork.class);
        assertNotSerialized(NeutronNetwork.class, network, "description");
        NeutronPort port = ObjectMapperSingleton.getContext(NeutronPort.class).readValue(PORT_DESC, NeutronPort.class);
        assertNotSerialized(NeutronPort.class, port, "description");
        org.openstack4j.openstack.networking.domain.ext.NeutronNetQosPolicy policy = ObjectMapperSingleton
                .getContext(org.openstack4j.openstack.networking.domain.ext.NeutronNetQosPolicy.class)
                .readValue(QOS_POLICY, org.openstack4j.openstack.networking.domain.ext.NeutronNetQosPolicy.class);
        assertNotSerialized(org.openstack4j.openstack.networking.domain.ext.NeutronNetQosPolicy.class, policy, "created_at", "updated_at");
        org.openstack4j.openstack.networking.domain.ext.NeutronNetworkIPAvailability availability = ObjectMapperSingleton
                .getContext(org.openstack4j.openstack.networking.domain.ext.NeutronNetworkIPAvailability.class)
                .readValue(AVAILABILITY, org.openstack4j.openstack.networking.domain.ext.NeutronNetworkIPAvailability.class);
        Assert.assertNotNull(availability.getIpAvailabilityDetails());
        assertNotSerialized(org.openstack4j.openstack.networking.domain.ext.NeutronNetworkIPAvailability.class, availability,
                "ip_availability_details", "total_ips_in_subnet");
    }

    private static void assertKeysAbsent(JsonNode node, String... keys) {
        for (String key : keys) {
            Assert.assertFalse(node.has(key), key + " was sent: " + node);
        }
    }

    private static void assertNotSerialized(Class<?> type, Object value, String... keys) throws Exception {
        String json = ObjectMapperSingleton.getContext(type).writeValueAsString(value);
        for (String key : keys) {
            String normalized = key.replace("_", "").toLowerCase(java.util.Locale.ROOT);
            Assert.assertFalse(json.replace("_", "").toLowerCase(java.util.Locale.ROOT).contains("\"" + normalized + "\""),
                    key + " was serialized: " + json);
        }
    }
}
