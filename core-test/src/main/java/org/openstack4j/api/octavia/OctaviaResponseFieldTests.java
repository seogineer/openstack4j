package org.openstack4j.api.octavia;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.api.Builders;
import org.openstack4j.api.octavia.ext.AbstractOctaviaExtTest;
import org.openstack4j.model.octavia.LbOperatingStatus;
import org.openstack4j.model.octavia.LbPoolV2;
import org.openstack4j.model.octavia.LbProvisioningStatus;
import org.openstack4j.model.octavia.ListenerV2;
import org.openstack4j.model.octavia.LoadBalancerV2Stats;
import org.openstack4j.model.octavia.MemberV2;
import org.openstack4j.openstack.octavia.domain.OctaviaLbPoolV2Update;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Octavia pool/listener/member/stats response fields and the pool TLS / member name create fields. */
@Test(suiteName = "Octavia/ResponseFields")
public class OctaviaResponseFieldTests extends AbstractOctaviaExtTest {

    private static final String POOL = "{\"pool\":{\"id\":\"pool1\",\"name\":\"pool\",\"protocol\":\"HTTP\",\"lb_algorithm\":\"ROUND_ROBIN\","
            + "\"ca_tls_container_ref\":\"https://barbican/containers/ca\",\"tls_ciphers\":\"ECDHE-RSA-AES256-GCM-SHA384\","
            + "\"tls_versions\":[\"TLSv1.2\",\"TLSv1.3\"],\"loadbalancers\":[{\"id\":\"lb1\"}],"
            + "\"operating_status\":\"ONLINE\",\"provisioning_status\":\"ACTIVE\"}}";
    private static final String LISTENER = "{\"listener\":{\"id\":\"l1\",\"name\":\"listener\",\"protocol\":\"HTTP\",\"protocol_port\":80,"
            + "\"provisioning_status\":\"PENDING_UPDATE\",\"operating_status\":\"OFFLINE\"}}";

    public void poolResponseFields() throws Exception {
        respondWith(200, POOL);
        LbPoolV2 pool = osv3().octavia().lbPoolV2().get("pool1");
        expect("GET", "/lbaas/pools/pool1");
        Assert.assertEquals(pool.getCaTlsContainerRef(), "https://barbican/containers/ca");
        Assert.assertEquals(pool.getTlsCiphers(), "ECDHE-RSA-AES256-GCM-SHA384");
        Assert.assertEquals(pool.getTlsVersions(), Arrays.asList("TLSv1.2", "TLSv1.3"));
        Assert.assertEquals(pool.getLoadBalancers().get(0).getId(), "lb1");
        Assert.assertEquals(pool.getOperatingStatus(), LbOperatingStatus.ONLINE);
        Assert.assertEquals(pool.getProvisioningStatus(), LbProvisioningStatus.ACTIVE);
    }

    public void poolCreateSendsTlsFieldsOnly() throws Exception {
        respondWith(200, POOL);
        respondWith(200, POOL);
        respondWith(200, POOL);
        LbPoolV2 created = osv3().octavia().lbPoolV2().create(Builders.octavia().lbPoolV2().name("pool")
                .tlsCiphers("ECDHE").tlsVersions(Arrays.asList("TLSv1.3")).caTlsContainerRef("ref").build());
        JsonNode sent = body(expect("POST", "/lbaas/pools")).get("pool");
        Assert.assertEquals(sent.get("tls_ciphers").asText(), "ECDHE");
        Assert.assertEquals(sent.get("tls_versions").get(0).asText(), "TLSv1.3");
        Assert.assertEquals(sent.get("ca_tls_container_ref").asText(), "ref");
        Assert.assertNotNull(created);

        // a response object reused as a request must not send the response-only fields
        LbPoolV2 fetched = osv3().octavia().lbPoolV2().get("pool1");
        expect("GET", "/lbaas/pools/pool1");
        osv3().octavia().lbPoolV2().create(fetched.toBuilder().name("copy").build());
        JsonNode copy = body(expect("POST", "/lbaas/pools")).get("pool");
        copy.fieldNames().forEachRemaining(key -> {
            String normalized = key.replace("_", "").toLowerCase(java.util.Locale.ROOT);
            Assert.assertFalse(normalized.equals("operatingstatus") || normalized.equals("provisioningstatus")
                    || normalized.equals("loadbalancers"), key + " was sent: " + copy);
        });
        Assert.assertEquals(copy.get("name").asText(), "copy");
    }

    public void listenerStatuses() throws Exception {
        respondWith(200, LISTENER);
        respondWith(200, LISTENER);
        ListenerV2 listener = osv3().octavia().listenerV2().get("l1");
        expect("GET", "/lbaas/listeners/l1");
        Assert.assertEquals(listener.getProvisioningStatus(), LbProvisioningStatus.PENDING_UPDATE);
        Assert.assertEquals(listener.getOperatingStatus(), LbOperatingStatus.OFFLINE);

        osv3().octavia().listenerV2().create(listener.toBuilder().name("copy").build());
        JsonNode sent = body(expect("POST", "/lbaas/listeners")).get("listener");
        Assert.assertFalse(sent.has("provisioning_status"), sent.toString());
        Assert.assertFalse(sent.has("operating_status"), sent.toString());
    }

    public void memberName() throws Exception {
        respondWith(200, "{\"member\":{\"id\":\"m1\",\"name\":\"from-server\",\"address\":\"10.0.0.8\",\"protocol_port\":80}}");
        respondWith(200, "{\"member\":{\"id\":\"m1\",\"name\":\"m\",\"address\":\"10.0.0.8\",\"protocol_port\":80}}");
        MemberV2 member = osv3().octavia().lbPoolV2().getMember("pool1", "m1");
        expect("GET", "/lbaas/pools/pool1/members/m1");
        Assert.assertEquals(member.getName(), "from-server");

        osv3().octavia().lbPoolV2().createMember("pool1", Builders.octavia().memberV2()
                .address("10.0.0.8").protocolPort(80).name("m").build());
        JsonNode sent = body(expect("POST", "/lbaas/pools/pool1/members")).get("member");
        Assert.assertEquals(sent.get("name").asText(), "m");
    }

    public void lbStatsRequestErrors() throws Exception {
        respondWith(200, "{\"stats\":{\"bytes_in\":1,\"bytes_out\":2,\"active_connections\":3,\"total_connections\":4,\"request_errors\":5}}");
        LoadBalancerV2Stats stats = osv3().octavia().loadBalancerV2().stats("lb1");
        expect("GET", "/lbaas/loadbalancers/lb1/stats");
        Assert.assertEquals(stats.getRequestErrors(), Long.valueOf(5));
        Assert.assertEquals(stats.getTotalConnections(), Integer.valueOf(4));
    }

    public void poolUpdateSendsTlsFields() throws Exception {
        respondWith(200, POOL);
        osv3().octavia().lbPoolV2().update("p1", OctaviaLbPoolV2Update.builder()
                .tlsCiphers("ECDHE-ECDSA-AES256-GCM-SHA384").tlsVersions(Arrays.asList("TLSv1.2", "TLSv1.3"))
                .caTlsContainerRef("ref").build());
        String raw = expect("PUT", "/lbaas/pools/p1").getBody().readUtf8();
        JsonNode sent = new ObjectMapper().readTree(raw).get("pool");
        Assert.assertEquals(sent.get("tls_ciphers").asText(), "ECDHE-ECDSA-AES256-GCM-SHA384");
        Assert.assertEquals(sent.get("tls_versions").get(1).asText(), "TLSv1.3");
        Assert.assertEquals(sent.get("ca_tls_container_ref").asText(), "ref");
        for (String leaked : new String[]{"tlsCiphers", "tlsVersions", "caTlsContainerRef"}) {
            Assert.assertFalse(raw.contains("\"" + leaked + "\""), raw);
        }
    }

    public void poolUpdateWithoutTlsUnchanged() throws Exception {
        respondWith(200, POOL);
        osv3().octavia().lbPoolV2().update("p1", OctaviaLbPoolV2Update.builder().name("x").build());
        JsonNode sent = new ObjectMapper().readTree(expect("PUT", "/lbaas/pools/p1").getBody().readUtf8()).get("pool");
        Assert.assertEquals(sent.get("name").asText(), "x");
        Assert.assertTrue(sent.get("admin_state_up").asBoolean(), sent.toString());
        for (String key : new String[]{"tls_ciphers", "tls_versions", "ca_tls_container_ref"}) {
            Assert.assertFalse(sent.has(key), sent.toString());
        }
    }

    public void listenerLoadBalancers() throws Exception {
        respondWith(200, "{\"listener\":{\"id\":\"l1\",\"loadbalancers\":[{\"id\":\"lb1\"}]}}");
        ListenerV2 listener = osv3().octavia().listenerV2().get("l1");
        expect("GET", "/lbaas/listeners/l1");
        Assert.assertEquals(listener.getLoadBalancers().get(0).getId(), "lb1");
    }

    public void listenerCreateDoesNotSendLoadBalancers() throws Exception {
        respondWith(200, "{\"listener\":{\"id\":\"l1\",\"loadbalancers\":[{\"id\":\"lb1\"}]}}");
        respondWith(200, "{\"listener\":{\"id\":\"l2\"}}");
        ListenerV2 listener = osv3().octavia().listenerV2().get("l1");
        expect("GET", "/lbaas/listeners/l1");
        osv3().octavia().listenerV2().create(listener.toBuilder().name("copy").build());
        String raw = expect("POST", "/lbaas/listeners").getBody().readUtf8();
        Assert.assertFalse(raw.toLowerCase(java.util.Locale.ROOT).contains("loadbalancers\""), raw);
        Assert.assertTrue(raw.contains("\"copy\""), raw);
    }
}
