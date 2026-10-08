package org.openstack4j.api.ext.small;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Telemetry/AodhExt")
public class AodhExtTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.TELEMETRY;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    public void historyStateQueryQuotas() throws Exception {
        respondWith(200, "[{\"alarm_id\": \"a1\", \"type\": \"state transition\", \"detail\": \"{\\\"state\\\": \\\"alarm\\\"}\", \"timestamp\": \"2024-01-01T00:00:00\"}]");
        respondWith(200, "\"ok\"");
        respondWith(200, "\"alarm\"");
        respondWith(200, "[{\"alarm_id\": \"a1\", \"name\": \"cpu_high\", \"state\": \"alarm\"}]");
        respondWith(200, "[{\"alarm_id\": \"a1\", \"type\": \"rule change\"}]");
        respondWith(200, "{\"project_id\": \"p1\", \"quotas\": [{\"resource\": \"alarms\", \"limit\": 30}]}");
        respondWith(201, "{\"project_id\": \"p1\", \"quotas\": [{\"resource\": \"alarms\", \"limit\": 50}]}");
        respondWith(204);

        var ext = osv3().telemetry().alarmsExt();
        List<Map<String, Object>> history = ext.history("a1", null);
        String state = ext.getState("a1");
        String newState = ext.setState("a1", "alarm");
        List<Map<String, Object>> alarms = ext.queryAlarms("{\"=\": {\"state\": \"alarm\"}}", "[{\"name\": \"asc\"}]", 10);
        ext.queryHistory("{\"=\": {\"type\": \"rule change\"}}", null, null);
        Map<String, Integer> quotas = ext.getQuotas("p1");
        Map<String, Integer> updated = ext.setQuotas("p1", Map.of("alarms", 50));
        Assert.assertTrue(ext.deleteQuotas("p1").isSuccess());

        Assert.assertTrue(path(takeRequest()).endsWith("/v2/alarms/a1/history"));
        Assert.assertTrue(path(takeRequest()).endsWith("/v2/alarms/a1/state"));
        RecordedRequest put = takeRequest();
        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertEquals(put.getBody().readUtf8(), "\"alarm\"");
        RecordedRequest query = takeRequest();
        Assert.assertTrue(path(query).endsWith("/v2/query/alarms"));
        var q = new ObjectMapper().readTree(query.getBody().readUtf8());
        Assert.assertEquals(q.get("filter").asText(), "{\"=\": {\"state\": \"alarm\"}}");
        Assert.assertEquals(q.get("limit").asInt(), 10);
        RecordedRequest queryHistory = takeRequest();
        Assert.assertTrue(path(queryHistory).endsWith("/v2/query/alarms/history"));
        Assert.assertFalse(new ObjectMapper().readTree(queryHistory.getBody().readUtf8()).has("limit"));
        Assert.assertTrue(path(takeRequest()).endsWith("/v2/quotas?project_id=p1"));
        RecordedRequest post = takeRequest();
        Assert.assertEquals(new ObjectMapper().readTree(post.getBody().readUtf8()).toString(), "{\"project_id\":\"p1\",\"quotas\":[{\"resource\":\"alarms\",\"limit\":50}]}");
        RecordedRequest delete = takeRequest();
        Assert.assertEquals(delete.getMethod(), "DELETE");
        Assert.assertTrue(path(delete).endsWith("/v2/quotas?project_id=p1"));
        Assert.assertEquals(history.get(0).get("type"), "state transition");
        Assert.assertEquals(state, "ok");
        Assert.assertEquals(newState, "alarm");
        Assert.assertEquals(alarms.get(0).get("name"), "cpu_high");
        Assert.assertEquals(quotas.get("alarms"), Integer.valueOf(30));
        Assert.assertEquals(updated.get("alarms"), Integer.valueOf(50));
    }
}
