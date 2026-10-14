package org.openstack4j.api.network;

import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.openstack4j.model.network.Agent;
import org.openstack4j.model.network.Subnet;
import org.testng.Assert;
import org.testng.annotations.Test;

/** subnet().list(Map) and agent().list(Map). */
@Test(suiteName = "Network/ListFilters")
public class NeutronListFilterTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.NETWORK;
    }

    private static Set<String> query(RecordedRequest request) {
        String query = request.getRequestUrl().encodedQuery();
        return query == null ? Set.of() : Set.of(query.split("&"));
    }

    public void subnetsWithFilters() throws Exception {
        respondWith(200, "{\"subnets\": [{\"id\": \"s1\", \"name\": \"sub\", \"network_id\": \"n1\", \"ip_version\": 4, \"cidr\": \"10.0.0.0/24\"}]}");

        List<? extends Subnet> subnets = osv3().networking().subnet().list(Map.of("network_id", "n1", "ip_version", "4"));

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "GET");
        Assert.assertEquals(request.getRequestUrl().encodedPath(), "/v2.0/subnets");
        Assert.assertEquals(query(request), Set.of("network_id=n1", "ip_version=4"));
        Assert.assertEquals(subnets.size(), 1);
        Assert.assertEquals(subnets.get(0).getNetworkId(), "n1");
    }

    public void subnetsNullAndPlainListSendNoQuery() throws Exception {
        respondWith(200, "{\"subnets\": []}");
        respondWith(200, "{\"subnets\": []}");

        Assert.assertTrue(osv3().networking().subnet().list(null).isEmpty());
        Assert.assertTrue(osv3().networking().subnet().list().isEmpty());

        Assert.assertEquals(query(takeRequest()), Set.of());
        Assert.assertEquals(query(takeRequest()), Set.of());
    }

    public void agentsWithEncodedFilters() throws Exception {
        respondWith(200, "{\"agents\": [{\"id\": \"a1\", \"agent_type\": \"DHCP agent\", \"host\": \"h1\", \"binary\": \"neutron-dhcp-agent\", \"alive\": true}]}");

        List<? extends Agent> agents = osv3().networking().agent().list(Map.of("agent_type", "DHCP agent", "host", "h1"));

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "GET");
        Assert.assertEquals(request.getRequestUrl().encodedPath(), "/v2.0/agents");
        Set<String> query = query(request);
        Assert.assertEquals(query.size(), 2);
        Assert.assertTrue(query.contains("host=h1"), query.toString());
        Assert.assertTrue(query.contains("agent_type=DHCP%20agent") || query.contains("agent_type=DHCP+agent"), query.toString());
        Assert.assertEquals(agents.size(), 1);
        Assert.assertEquals(agents.get(0).getHost(), "h1");
    }

    public void agentsNullAndPlainListSendNoQuery() throws Exception {
        respondWith(200, "{\"agents\": []}");
        respondWith(200, "{\"agents\": []}");

        Assert.assertTrue(osv3().networking().agent().list(null).isEmpty());
        Assert.assertTrue(osv3().networking().agent().list().isEmpty());

        Assert.assertEquals(query(takeRequest()), Set.of());
        Assert.assertEquals(query(takeRequest()), Set.of());
    }
}
