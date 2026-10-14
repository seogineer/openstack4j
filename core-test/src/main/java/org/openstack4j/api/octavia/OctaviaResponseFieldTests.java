package org.openstack4j.api.octavia;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.api.Builders;
import org.openstack4j.api.octavia.ext.AbstractOctaviaExtTest;
import org.openstack4j.model.octavia.HealthMonitorV2;
import org.openstack4j.model.octavia.LbOperatingStatus;
import org.openstack4j.model.octavia.LbPoolV2;
import org.openstack4j.model.octavia.LbProvisioningStatus;
import org.openstack4j.model.octavia.ListenerV2;
import org.openstack4j.model.octavia.LoadBalancerV2;
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

    private static final String LB_FULL = "{\"loadbalancer\":{\"id\":\"lb1\",\"name\":\"lb\",\"provisioning_status\":\"ACTIVE\",\"operating_status\":\"ONLINE\","
            + "\"created_at\":\"2017-05-11T17:21:34\",\"updated_at\":\"2017-05-11T17:21:35\"}}";
    private static final String LISTENER_FULL = "{\"listener\":{\"id\":\"l1\",\"name\":\"listener\",\"protocol\":\"HTTP\",\"protocol_port\":80,"
            + "\"created_at\":\"2017-05-11T17:21:34\",\"updated_at\":\"2017-05-11T17:21:35\","
            + "\"insert_headers\":{\"X-Forwarded-For\":\"true\"},\"timeout_client_data\":50000,\"timeout_member_connect\":5000,"
            + "\"timeout_member_data\":50000,\"timeout_tcp_inspect\":0,\"provisioning_status\":\"ACTIVE\",\"operating_status\":\"ONLINE\"}}";
    private static final String POOL_FULL = "{\"pool\":{\"id\":\"pool1\",\"name\":\"pool\",\"protocol\":\"HTTP\",\"lb_algorithm\":\"ROUND_ROBIN\","
            + "\"created_at\":\"2017-05-11T17:21:34\",\"updated_at\":\"2017-05-11T17:21:35\",\"tls_enabled\":true,"
            + "\"operating_status\":\"ONLINE\",\"provisioning_status\":\"ACTIVE\"}}";
    private static final String MEMBER_FULL = "{\"member\":{\"id\":\"m1\",\"name\":\"member\",\"address\":\"10.0.0.8\",\"protocol_port\":80,\"weight\":1,"
            + "\"created_at\":\"2017-05-11T17:21:34\",\"updated_at\":\"2017-05-11T17:21:35\","
            + "\"operating_status\":\"ERROR\",\"provisioning_status\":\"ACTIVE\",\"backup\":true,"
            + "\"monitor_address\":\"10.0.0.9\",\"monitor_port\":8080}}";
    private static final String HM_FULL = "{\"healthmonitor\":{\"id\":\"hm1\",\"name\":\"monitor\",\"type\":\"HTTP\",\"delay\":5,\"timeout\":3,\"max_retries\":3,"
            + "\"created_at\":\"2017-05-11T17:21:34\",\"updated_at\":\"2017-05-11T17:21:35\","
            + "\"operating_status\":\"ONLINE\",\"provisioning_status\":\"PENDING_UPDATE\"}}";
    private static final long CREATED = 1494523294000L;
    private static final long UPDATED = 1494523295000L;
    private static final String[] LB_KEYS = {"created_at", "updated_at"};
    private static final String[] LISTENER_KEYS = {"created_at", "updated_at", "insert_headers", "timeout_client_data", "timeout_member_connect",
            "timeout_member_data", "timeout_tcp_inspect"};
    private static final String[] POOL_KEYS = {"created_at", "updated_at", "tls_enabled"};
    private static final String[] MEMBER_KEYS = {"created_at", "updated_at", "operating_status", "provisioning_status", "backup", "monitor_address", "monitor_port"};
    private static final String[] HM_KEYS = {"created_at", "updated_at", "name", "operating_status", "provisioning_status"};

    public void loadBalancerTimestamps() throws Exception {
        respondWith(200, LB_FULL);
        LoadBalancerV2 lb = osv3().octavia().loadBalancerV2().get("lb1");
        expect("GET", "/lbaas/loadbalancers/lb1");
        Assert.assertEquals(lb.getCreatedAt().getTime(), CREATED);
        Assert.assertEquals(lb.getUpdatedAt().getTime(), UPDATED);
    }

    public void listenerNewResponseFields() throws Exception {
        respondWith(200, LISTENER_FULL);
        ListenerV2 listener = osv3().octavia().listenerV2().get("l1");
        expect("GET", "/lbaas/listeners/l1");
        Assert.assertEquals(listener.getCreatedAt().getTime(), CREATED);
        Assert.assertEquals(listener.getUpdatedAt().getTime(), UPDATED);
        Assert.assertEquals(listener.getInsertHeaders().get("X-Forwarded-For"), "true");
        Assert.assertEquals(listener.getTimeoutClientData(), Integer.valueOf(50000));
        Assert.assertEquals(listener.getTimeoutMemberConnect(), Integer.valueOf(5000));
        Assert.assertEquals(listener.getTimeoutMemberData(), Integer.valueOf(50000));
        Assert.assertEquals(listener.getTimeoutTcpInspect(), Integer.valueOf(0));
    }

    public void poolNewResponseFields() throws Exception {
        respondWith(200, POOL_FULL);
        LbPoolV2 pool = osv3().octavia().lbPoolV2().get("pool1");
        expect("GET", "/lbaas/pools/pool1");
        Assert.assertEquals(pool.getCreatedAt().getTime(), CREATED);
        Assert.assertEquals(pool.getUpdatedAt().getTime(), UPDATED);
        Assert.assertEquals(pool.getTlsEnabled(), Boolean.TRUE);
    }

    public void memberNewResponseFields() throws Exception {
        respondWith(200, MEMBER_FULL);
        MemberV2 member = osv3().octavia().lbPoolV2().getMember("pool1", "m1");
        expect("GET", "/lbaas/pools/pool1/members/m1");
        Assert.assertEquals(member.getCreatedAt().getTime(), CREATED);
        Assert.assertEquals(member.getUpdatedAt().getTime(), UPDATED);
        Assert.assertEquals(member.getOperatingStatus(), LbOperatingStatus.ERROR);
        Assert.assertEquals(member.getProvisioningStatus(), LbProvisioningStatus.ACTIVE);
        Assert.assertEquals(member.getBackup(), Boolean.TRUE);
        Assert.assertEquals(member.getMonitorAddress(), "10.0.0.9");
        Assert.assertEquals(member.getMonitorPort(), Integer.valueOf(8080));
        Assert.assertEquals(member.getName(), "member");
    }

    public void healthMonitorNewResponseFields() throws Exception {
        respondWith(200, HM_FULL);
        HealthMonitorV2 monitor = osv3().octavia().healthMonitorV2().get("hm1");
        expect("GET", "/lbaas/healthmonitors/hm1");
        Assert.assertEquals(monitor.getName(), "monitor");
        Assert.assertEquals(monitor.getOperatingStatus(), LbOperatingStatus.ONLINE);
        Assert.assertEquals(monitor.getProvisioningStatus(), LbProvisioningStatus.PENDING_UPDATE);
        Assert.assertEquals(monitor.getCreatedAt().getTime(), CREATED);
        Assert.assertEquals(monitor.getUpdatedAt().getTime(), UPDATED);
    }

    public void newFieldsAreNullWhenAbsent() throws Exception {
        respondWith(200, "{\"member\":{\"id\":\"m1\",\"address\":\"10.0.0.8\",\"protocol_port\":80}}");
        respondWith(200, "{\"healthmonitor\":{\"id\":\"hm1\",\"type\":\"HTTP\"}}");
        respondWith(200, "{\"listener\":{\"id\":\"l1\"}}");
        MemberV2 member = osv3().octavia().lbPoolV2().getMember("pool1", "m1");
        HealthMonitorV2 monitor = osv3().octavia().healthMonitorV2().get("hm1");
        ListenerV2 listener = osv3().octavia().listenerV2().get("l1");
        expect("GET", "/lbaas/pools/pool1/members/m1");
        expect("GET", "/lbaas/healthmonitors/hm1");
        expect("GET", "/lbaas/listeners/l1");
        Assert.assertNull(member.getBackup());
        Assert.assertNull(member.getOperatingStatus());
        Assert.assertNull(member.getMonitorPort());
        Assert.assertNull(monitor.getName());
        Assert.assertNull(monitor.getCreatedAt());
        Assert.assertNull(listener.getInsertHeaders());
        Assert.assertNull(listener.getTimeoutClientData());
    }

    public void responseObjectsReusedInCreateDoNotResendNewFields() throws Exception {
        respondWith(200, LB_FULL);
        respondWith(200, LISTENER_FULL);
        respondWith(200, POOL_FULL);
        respondWith(200, MEMBER_FULL);
        respondWith(200, HM_FULL);
        LoadBalancerV2 lb = osv3().octavia().loadBalancerV2().get("lb1");
        ListenerV2 listener = osv3().octavia().listenerV2().get("l1");
        LbPoolV2 pool = osv3().octavia().lbPoolV2().get("pool1");
        MemberV2 member = osv3().octavia().lbPoolV2().getMember("pool1", "m1");
        HealthMonitorV2 monitor = osv3().octavia().healthMonitorV2().get("hm1");
        expect("GET", "/lbaas/loadbalancers/lb1");
        expect("GET", "/lbaas/listeners/l1");
        expect("GET", "/lbaas/pools/pool1");
        expect("GET", "/lbaas/pools/pool1/members/m1");
        expect("GET", "/lbaas/healthmonitors/hm1");

        respondWith(201, LB_FULL);
        respondWith(201, LISTENER_FULL);
        respondWith(201, POOL_FULL);
        respondWith(201, MEMBER_FULL);
        respondWith(201, HM_FULL);
        osv3().octavia().loadBalancerV2().create(lb);
        osv3().octavia().listenerV2().create(listener);
        osv3().octavia().lbPoolV2().create(pool);
        osv3().octavia().lbPoolV2().createMember("pool1", member);
        osv3().octavia().healthMonitorV2().create(monitor);

        assertKeysAbsent(body(expect("POST", "/lbaas/loadbalancers")).get("loadbalancer"), LB_KEYS);
        assertKeysAbsent(body(expect("POST", "/lbaas/listeners")).get("listener"), LISTENER_KEYS);
        assertKeysAbsent(body(expect("POST", "/lbaas/pools")).get("pool"), POOL_KEYS);
        assertKeysAbsent(body(expect("POST", "/lbaas/pools/pool1/members")).get("member"), MEMBER_KEYS);
        assertKeysAbsent(body(expect("POST", "/lbaas/healthmonitors")).get("healthmonitor"), HM_KEYS);
    }

    public void builderCreateBodiesUnchanged() throws Exception {
        respondWith(201, POOL_FULL);
        respondWith(201, MEMBER_FULL);
        osv3().octavia().lbPoolV2().create(Builders.octavia().lbPoolV2().name("pool").protocol(org.openstack4j.model.octavia.Protocol.HTTP)
                .lbMethod(org.openstack4j.model.octavia.LbMethod.ROUND_ROBIN).tlsCiphers("ECDHE").tlsVersions(Arrays.asList("TLSv1.3"))
                .caTlsContainerRef("ref").build());
        osv3().octavia().lbPoolV2().createMember("pool1", Builders.octavia().memberV2().address("10.0.0.8").protocolPort(80).name("m").build());

        JsonNode pool = body(expect("POST", "/lbaas/pools")).get("pool");
        assertKeysAbsent(pool, POOL_KEYS);
        Assert.assertEquals(pool.get("tls_ciphers").asText(), "ECDHE");
        Assert.assertEquals(pool.get("name").asText(), "pool");
        JsonNode member = body(expect("POST", "/lbaas/pools/pool1/members")).get("member");
        assertKeysAbsent(member, MEMBER_KEYS);
        Assert.assertEquals(member.get("name").asText(), "m");
        Assert.assertEquals(member.get("address").asText(), "10.0.0.8");
    }

    public void newFieldsNotSerialized() throws Exception {
        assertNotSerialized(org.openstack4j.openstack.octavia.domain.OctaviaLoadBalancerV2.class, LB_FULL, LB_KEYS);
        assertNotSerialized(org.openstack4j.openstack.octavia.domain.OctaviaListenerV2.class, LISTENER_FULL, LISTENER_KEYS);
        assertNotSerialized(org.openstack4j.openstack.octavia.domain.OctaviaLbPoolV2.class, POOL_FULL, POOL_KEYS);
        assertNotSerialized(org.openstack4j.openstack.octavia.domain.OctaviaMemberV2.class, MEMBER_FULL, MEMBER_KEYS);
        assertNotSerialized(org.openstack4j.openstack.octavia.domain.OctaviaHealthMonitorV2.class, HM_FULL, HM_KEYS);
    }

    private static void assertKeysAbsent(JsonNode node, String... keys) {
        for (String key : keys) {
            String normalized = key.replace("_", "").toLowerCase(java.util.Locale.ROOT);
            node.fieldNames().forEachRemaining(field -> Assert.assertNotEquals(field.replace("_", "").toLowerCase(java.util.Locale.ROOT), normalized,
                    key + " was sent: " + node));
        }
    }

    private static void assertNotSerialized(Class<?> type, String json, String... keys) throws Exception {
        Object value = org.openstack4j.core.transport.ObjectMapperSingleton.getContext(type).readValue(json, type);
        String written = org.openstack4j.core.transport.ObjectMapperSingleton.getContext(type).writeValueAsString(value);
        for (String key : keys) {
            String normalized = key.replace("_", "").toLowerCase(java.util.Locale.ROOT);
            Assert.assertFalse(written.replace("_", "").toLowerCase(java.util.Locale.ROOT).contains("\"" + normalized + "\""), key + " was serialized: " + written);
        }
    }
}
