package org.openstack4j.api.messaging;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Messaging")
public class MessagingTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.MESSAGING;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    private static String json(RecordedRequest r) throws Exception {
        return new ObjectMapper().readTree(r.getBody().readUtf8()).toString();
    }

    public void queuesAndMessages() throws Exception {
        respondWith(201);
        respondWith(200, "{\"queues\": [{\"href\": \"/v2/queues/demo\", \"name\": \"demo\"}], \"links\": [], \"count\": 1}");
        respondWith(200, "{\"_default_message_ttl\": 3600, \"description\": \"billing\"}");
        respondWith(200, "{\"_default_message_ttl\": 3600, \"max_timeout\": 100}");
        respondWith(200, "{\"messages\": {\"claimed\": 10, \"total\": 20, \"free\": 10}}");
        respondWith(204);
        respondWith(201, "{\"resources\": [\"/v2/queues/demo/messages/m1\", \"/v2/queues/demo/messages/m2\"]}");
        respondWith(200, "{\"messages\": [{\"id\": \"m1\", \"body\": {\"event\": \"BackupStarted\"}, \"ttl\": 300, \"age\": 1}], \"links\": []}");
        respondWith(204);
        respondWith(200, "{\"messages\": [{\"id\": \"m2\", \"claim_count\": 1}]}");
        respondWith(204);
        respondWith(204);

        var mq = osv3().messaging();
        mq.useClientId("3381af92-2b9e-11e3-b191-71861300734c");
        Assert.assertTrue(mq.createQueue("demo", Map.of("_default_message_ttl", 3600)).isSuccess());
        Map<String, Object> queues = mq.listQueues(Map.of("detailed", "true"));
        Map<String, Object> queue = mq.getQueue("demo");
        Map<String, Object> updated = mq.updateQueue("demo", List.of(Map.of("op", "replace", "path", "/metadata/max_timeout", "value", 100)));
        Map<String, Object> stats = mq.queueStats("demo");
        Assert.assertTrue(mq.purgeQueue("demo", null).isSuccess());
        List<String> posted = mq.postMessages("demo", List.of(Map.of("ttl", 300, "body", Map.of("event", "BackupStarted"))));
        Map<String, Object> messages = mq.listMessages("demo", Map.of("echo", "true"));
        Map<String, Object> empty = mq.listMessages("demo", null);
        List<Map<String, Object>> popped = mq.popMessages("demo", 1);
        Assert.assertTrue(mq.deleteMessage("demo", "m1", "c1").isSuccess());
        Assert.assertTrue(mq.deleteQueue("demo").isSuccess());

        RecordedRequest create = takeRequest();
        Assert.assertEquals(create.getMethod(), "PUT");
        Assert.assertEquals(path(create), "/v2/queues/demo");
        Assert.assertEquals(create.getHeader("Client-ID"), "3381af92-2b9e-11e3-b191-71861300734c");
        Assert.assertEquals(json(create), "{\"_default_message_ttl\":3600}");
        Assert.assertEquals(path(takeRequest()), "/v2/queues?detailed=true");
        Assert.assertEquals(path(takeRequest()), "/v2/queues/demo");
        RecordedRequest patch = takeRequest();
        Assert.assertEquals(patch.getMethod(), "PATCH");
        Assert.assertTrue(patch.getHeader("Content-Type").startsWith("application/openstack-messaging-v2.0-json-patch"), patch.getHeader("Content-Type"));
        var ops = new ObjectMapper().readTree(patch.getBody().readUtf8());
        Assert.assertEquals(ops.get(0).get("path").asText(), "/metadata/max_timeout");
        Assert.assertEquals(ops.get(0).get("value").asInt(), 100);
        Assert.assertEquals(path(takeRequest()), "/v2/queues/demo/stats");
        Assert.assertEquals(json(takeRequest()), "{\"resource_types\":[\"messages\",\"subscriptions\"]}");
        RecordedRequest post = takeRequest();
        Assert.assertEquals(path(post), "/v2/queues/demo/messages");
        Assert.assertTrue(json(post).startsWith("{\"messages\":[{"), json(post));
        Assert.assertEquals(path(takeRequest()), "/v2/queues/demo/messages?echo=true");
        takeRequest();
        Assert.assertEquals(path(takeRequest()), "/v2/queues/demo/messages?pop=1");
        Assert.assertEquals(path(takeRequest()), "/v2/queues/demo/messages/m1?claim_id=c1");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(((List<?>) queues.get("queues")).size(), 1);
        Assert.assertEquals(queue.get("description"), "billing");
        Assert.assertEquals(updated.get("max_timeout"), 100);
        Assert.assertEquals(((Map<?, ?>) stats.get("messages")).get("total"), 20);
        Assert.assertEquals(posted, List.of("/v2/queues/demo/messages/m1", "/v2/queues/demo/messages/m2"));
        Assert.assertEquals(((Map<?, ?>) ((List<?>) messages.get("messages")).get(0)).get("id"), "m1");
        Assert.assertEquals(messages.get("links"), List.of());
        Assert.assertEquals(empty.get("messages"), List.of());
        Assert.assertEquals(popped.get(0).get("claim_count"), 1);
    }

    public void claimsSubscriptionsPoolsFlavorsHealth() throws Exception {
        respondWith(Map.of("Content-Type", "application/json", "Location", "/v2/queues/demo/claims/c1"), 201,
                "{\"messages\": [{\"body\": {\"event\": \"x\"}, \"href\": \"/v2/queues/demo/messages/m1?claim_id=c1\", \"ttl\": 300}]}");
        respondWith(204);
        respondWith(200, "{\"age\": 57, \"ttl\": 300, \"messages\": []}");
        respondWith(204);
        respondWith(204);
        respondWith(201, "{\"subscription_id\": \"s1\"}");
        respondWith(200, "{\"subscriptions\": [{\"id\": \"s1\", \"subscriber\": \"http://10.229.49.117:5679\"}], \"links\": []}");
        respondWith(204);
        respondWith(201);
        respondWith(204);
        respondWith(201);
        respondWith(200, "{\"pools\": [{\"name\": \"p1\", \"weight\": 100, \"uri\": \"mongodb://127.0.0.1:27017\"}]}");
        respondWith(201);
        respondWith(200, "{\"name\": \"f1\", \"pool_list\": \"[p1]\"}");
        respondWith(204);
        respondWith(200, "{\"catalog_reachable\": true}");

        var mq = osv3().messaging();
        Map<String, Object> claimed = mq.claimMessages("demo", 300, 300, 5);
        Map<String, Object> none = mq.claimMessages("demo", 300, 300, null);
        Map<String, Object> claim = mq.getClaim("demo", "c1");
        Assert.assertTrue(mq.updateClaim("demo", "c1", 600, null).isSuccess());
        Assert.assertTrue(mq.releaseClaim("demo", "c1").isSuccess());
        String subscription = mq.createSubscription("demo", "http://10.229.49.117:5679", 3600, Map.of());
        Assert.assertTrue(mq.listSubscriptions("demo", null).containsKey("links"));
        Assert.assertTrue(mq.updateSubscription("demo", "s1", Map.of("ttl", 360)).isSuccess());
        Assert.assertTrue(mq.confirmSubscription("demo", "s1", true).isSuccess());
        Assert.assertTrue(mq.deleteSubscription("demo", "s1").isSuccess());
        Assert.assertTrue(mq.createPool("p1", Map.of("weight", 100, "uri", "mongodb://127.0.0.1:27017")).isSuccess());
        Map<String, Object> pools = mq.listPools(null);
        Assert.assertTrue(mq.createFlavor("f1", Map.of("pool_list", List.of("p1"))).isSuccess());
        Map<String, Object> flavor = mq.getFlavor("f1");
        Assert.assertTrue(mq.ping());
        Map<String, Object> health = mq.health();

        RecordedRequest claimRequest = takeRequest();
        Assert.assertEquals(path(claimRequest), "/v2/queues/demo/claims?limit=5");
        Assert.assertEquals(json(claimRequest), "{\"ttl\":300,\"grace\":300}");
        Assert.assertEquals(path(takeRequest()), "/v2/queues/demo/claims");
        Assert.assertEquals(path(takeRequest()), "/v2/queues/demo/claims/c1");
        RecordedRequest updateClaim = takeRequest();
        Assert.assertEquals(updateClaim.getMethod(), "PATCH");
        Assert.assertEquals(json(updateClaim), "{\"ttl\":600}");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertTrue(json(takeRequest()).contains("\"subscriber\":\"http://10.229.49.117:5679\""));
        Assert.assertEquals(path(takeRequest()), "/v2/queues/demo/subscriptions");
        Assert.assertEquals(takeRequest().getMethod(), "PATCH");
        RecordedRequest confirm = takeRequest();
        Assert.assertEquals(confirm.getMethod(), "PUT");
        Assert.assertEquals(path(confirm), "/v2/queues/demo/subscriptions/s1/confirm");
        Assert.assertEquals(json(confirm), "{\"confirmed\":true}");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        RecordedRequest pool = takeRequest();
        Assert.assertEquals(pool.getMethod(), "PUT");
        Assert.assertEquals(path(pool), "/v2/pools/p1");
        Assert.assertEquals(path(takeRequest()), "/v2/pools");
        Assert.assertEquals(path(takeRequest()), "/v2/flavors/f1");
        Assert.assertEquals(path(takeRequest()), "/v2/flavors/f1");
        Assert.assertEquals(path(takeRequest()), "/v2/ping");
        Assert.assertEquals(path(takeRequest()), "/v2/health");
        Assert.assertEquals(claimed.get("claim_id"), "c1");
        Assert.assertEquals(((Map<?, ?>) ((List<?>) claimed.get("messages")).get(0)).get("ttl"), 300);
        Assert.assertNull(none.get("claim_id"));
        Assert.assertEquals(none.get("messages"), List.of());
        Assert.assertEquals(claim.get("age"), 57);
        Assert.assertEquals(subscription, "s1");
        Assert.assertEquals(((Map<?, ?>) ((List<?>) pools.get("pools")).get(0)).get("name"), "p1");
        Assert.assertEquals(flavor.get("pool_list"), "[p1]");
        Assert.assertEquals(health.get("catalog_reachable"), Boolean.TRUE);
    }

    public void getMessagesWhenNoneExistIsEmpty() throws Exception {
        respondWith(404, "{\"title\": \"Not found\", \"description\": \"Messages could not be found.\"}");
        Assert.assertTrue(osv3().messaging().getMessages("demo", List.of("gone")).isEmpty());
        takeRequest();
    }

    public void clientIdIsPerSessionAndSessionHeaderWins() throws Exception {
        respondWith(204);
        respondWith(204);
        respondWith(204);

        var first = osv3();
        first.messaging().useClientId("3381af92-2b9e-11e3-b191-71861300734c");
        first.messaging().deleteQueue("a");
        Map<String, String> previous = ((org.openstack4j.openstack.internal.OSClientSession<?, ?>) first).getHeaders();
        first.headers(Map.of("Client-ID", "9f3c6a1e-0000-4000-8000-000000000001"));
        try {
            first.messaging().deleteQueue("b");
        } finally {
            first.headers(previous);
        }
        var second = org.openstack4j.openstack.OSFactory.clientFromToken(first.getToken());
        second.messaging().deleteQueue("c");

        Assert.assertEquals(takeRequest().getHeader("Client-ID"), "3381af92-2b9e-11e3-b191-71861300734c");
        Assert.assertEquals(takeRequest().getHeaders().values("Client-ID"), List.of("9f3c6a1e-0000-4000-8000-000000000001"));
        String other = takeRequest().getHeader("Client-ID");
        Assert.assertNotNull(other);
        Assert.assertNotEquals(other, "3381af92-2b9e-11e3-b191-71861300734c");
        Assert.assertThrows(IllegalArgumentException.class, () -> second.messaging().useClientId("not-a-uuid"));
    }
}
