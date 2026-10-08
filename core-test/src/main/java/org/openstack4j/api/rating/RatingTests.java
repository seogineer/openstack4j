package org.openstack4j.api.rating;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Rating")
public class RatingTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.RATING;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    private static String json(RecordedRequest r) throws Exception {
        return new ObjectMapper().readTree(r.getBody().readUtf8()).toString();
    }

    public void ratingApis() throws Exception {
        respondWith(200, "{\"total\": 1, \"dataframes\": [{\"usage\": {}, \"period\": {\"begin\": \"2019-07-23T12:28:10+00:00\"}}]}");
        respondWith(204);
        respondWith(200, "{\"modules\": [{\"module_id\": \"hashmap\", \"enabled\": true, \"priority\": 1}]}");
        respondWith(200, "{\"module_id\": \"hashmap\", \"enabled\": true, \"priority\": 1, \"hot_config\": true}");
        respondWith(204);
        respondWith(200, "{\"results\": [{\"scope_id\": \"p1\", \"scope_key\": \"project_id\", \"active\": true}], \"total\": 1}");
        respondWith(202);
        respondWith(200, "{\"scope_id\": \"p1\", \"active\": false}");
        respondWith(200, "{\"total\": 1, \"columns\": [\"begin\", \"end\", \"qty\", \"rate\"], \"results\": [[\"2019-06-01T00:00:00Z\", \"2019-07-01T00:00:00Z\", 2590.4, 1295.2]]}");
        respondWith(202);
        respondWith(200, "{\"results\": [{\"scope_id\": \"p1\", \"reason\": \"fix\"}], \"total\": 1}");
        respondWith(200, "{\"results\": [{\"scope_id\": \"p1\", \"reason\": \"fix\"}], \"total\": 1}");

        var rating = osv3().rating();
        Map<String, Object> dataframes = rating.listDataframes(Map.of("begin", "2019-07-01T00:00:00Z"));
        Assert.assertTrue(rating.addDataframes(List.of(Map.of("period", Map.of()))).isSuccess());
        List<Map<String, Object>> modules = rating.listModules();
        Map<String, Object> module = rating.getModule("hashmap");
        Assert.assertTrue(rating.updateModule("hashmap", false, null).isSuccess());
        List<Map<String, Object>> scopes = rating.listScopes(Map.of("scope_key", "project_id"));
        Assert.assertTrue(rating.resetScopes(Map.of("all_scopes", true, "last_processed_timestamp", "2019-07-01T00:00:00Z")).isSuccess());
        Map<String, Object> scope = rating.patchScope(Map.of("scope_id", "p1", "active", false));
        Map<String, Object> summary = rating.summary(Map.of("groupby", "time"));
        Assert.assertTrue(rating.reprocess(List.of("p1"), "2021-06-01 00:00:00+00:00", "2021-06-01 23:00:00+00:00", "fix").isSuccess());
        rating.listReprocesses(Map.of("order", "asc"));
        List<Map<String, Object>> tasks = rating.getReprocesses("p1");

        Assert.assertEquals(path(takeRequest()), "/v2/dataframes?begin=2019-07-01T00:00:00Z");
        Assert.assertEquals(json(takeRequest()), "{\"dataframes\":[{\"period\":{}}]}");
        Assert.assertEquals(path(takeRequest()), "/v2/rating/modules");
        Assert.assertEquals(path(takeRequest()), "/v2/rating/modules/hashmap");
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        Assert.assertEquals(json(update), "{\"enabled\":false}");
        Assert.assertEquals(path(takeRequest()), "/v2/scope?scope_key=project_id");
        Assert.assertEquals(takeRequest().getMethod(), "PUT");
        Assert.assertEquals(takeRequest().getMethod(), "PATCH");
        Assert.assertEquals(path(takeRequest()), "/v2/summary?groupby=time");
        RecordedRequest reprocess = takeRequest();
        Assert.assertEquals(path(reprocess), "/v2/task/reprocesses");
        Assert.assertEquals(new ObjectMapper().readTree(reprocess.getBody().readUtf8()).get("scope_ids").get(0).asText(), "p1");
        Assert.assertEquals(path(takeRequest()), "/v2/task/reprocesses?order=asc");
        Assert.assertEquals(path(takeRequest()), "/v2/task/reprocesses/p1");
        Assert.assertEquals(dataframes.get("total"), 1);
        Assert.assertEquals(modules.get(0).get("module_id"), "hashmap");
        Assert.assertEquals(module.get("hot_config"), Boolean.TRUE);
        Assert.assertEquals(scopes.get(0).get("active"), Boolean.TRUE);
        Assert.assertEquals(scope.get("active"), Boolean.FALSE);
        Assert.assertEquals(((List<?>) summary.get("columns")).size(), 4);
        Assert.assertEquals(tasks.get(0).get("reason"), "fix");
    }
}
