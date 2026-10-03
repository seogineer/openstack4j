package org.openstack4j.api.identity.v3.ext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.Limit;
import org.openstack4j.model.identity.v3.RegisteredLimit;
import org.openstack4j.model.identity.v3.options.LimitCreate;
import org.openstack4j.model.identity.v3.options.LimitListOptions;
import org.openstack4j.model.identity.v3.options.RegisteredLimitCreate;
import org.openstack4j.model.identity.v3.options.RegisteredLimitListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/Limits")
public class LimitTests extends AbstractIdentityExtTest {

    private static final String SERVICE = "78d4693e713c43abb7f24f39020f9d76";
    private static final String RL = "3facd55c5d7d4ac88a8b4177a9c4e9d0";
    private static final String RL_JSON = "{\"id\": \"" + RL + "\", \"service_id\": \"" + SERVICE + "\", \"region_id\": null, \"resource_name\": \"os4j_fixture\", \"default_limit\": 5, \"description\": \"fixture\", \"links\": {}}";
    private static final String L_JSON = "{\"id\": \"l1\", \"service_id\": \"" + SERVICE + "\", \"region_id\": \"RegionOne\", \"resource_name\": \"os4j_fixture\", \"resource_limit\": 10, \"description\": null, \"project_id\": \"" + PROJECT + "\", \"domain_id\": null, \"links\": {}}";

    public void registeredLimits() throws Exception {
        respondWith(201, "{\"registered_limits\": [" + RL_JSON + "]}");
        respondWith(200, "{\"registered_limits\": [" + RL_JSON + "], \"links\": {}}");
        respondWith(200, "{\"registered_limits\": [], \"links\": {}}");
        respondWith(200, "{\"registered_limit\": " + RL_JSON + "}");
        respondWith(200, "{\"registered_limit\": " + RL_JSON + "}");
        respondWith(204);

        var limits = osv3().identity().registeredLimits();
        List<? extends RegisteredLimit> created = limits.create(Collections.singletonList(
                RegisteredLimitCreate.create(SERVICE, "os4j_fixture", 5).description("fixture")));
        limits.list();
        limits.list(RegisteredLimitListOptions.create().serviceId(SERVICE).resourceName("os4j_fixture"));
        RegisteredLimit one = limits.get(RL);
        limits.update(RL, 7, null);
        limits.delete(RL);

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/v3/registered_limits"));
        JsonNode first = body(create).get("registered_limits").get(0);
        Assert.assertEquals(first.get("service_id").asText(), SERVICE);
        Assert.assertEquals(first.get("default_limit").asInt(), 5);
        Assert.assertFalse(first.has("region_id"));
        Assert.assertEquals(takeRequest().getMethod(), "GET");
        String query = decodedPath(takeRequest());
        Assert.assertTrue(query.contains("service_id=" + SERVICE) && query.contains("resource_name=os4j_fixture"), query);
        Assert.assertTrue(takeRequest().getPath().endsWith("/registered_limits/" + RL));
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PATCH");
        Assert.assertEquals(body(update).get("registered_limit").get("default_limit").asInt(), 7);
        Assert.assertFalse(body(update).get("registered_limit").has("description"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.get(0).getDefaultLimit(), Integer.valueOf(5));
        Assert.assertNull(one.getRegionId());
    }

    public void projectLimitsAndModel() throws Exception {
        respondWith(200, "{\"model\": {\"name\": \"flat\", \"description\": \"Limit enforcement and validation does not take project hierarchy into consideration.\"}}");
        respondWith(201, "{\"limits\": [" + L_JSON + "]}");
        respondWith(200, "{\"limits\": [" + L_JSON + "], \"links\": {}}");
        respondWith(200, "{\"limit\": " + L_JSON + "}");
        respondWith(200, "{\"limit\": " + L_JSON + "}");
        respondWith(204);

        var limits = osv3().identity().limits();
        Assert.assertEquals(limits.model().getName(), "flat");
        List<? extends Limit> created = limits.create(Arrays.asList(
                LimitCreate.forProject(PROJECT, SERVICE, "os4j_fixture", 10).regionId("RegionOne")));
        limits.list(LimitListOptions.create().projectId(PROJECT));
        Limit one = limits.get("l1");
        limits.update("l1", null, "note");
        limits.delete("l1");

        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/limits/model"));
        JsonNode first = body(takeRequest()).get("limits").get(0);
        Assert.assertEquals(first.get("project_id").asText(), PROJECT);
        Assert.assertEquals(first.get("resource_limit").asInt(), 10);
        Assert.assertEquals(first.get("region_id").asText(), "RegionOne");
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v3/limits?project_id=" + PROJECT));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/limits/l1"));
        RecordedRequest update = takeRequest();
        Assert.assertEquals(body(update).get("limit").get("description").asText(), "note");
        Assert.assertFalse(body(update).get("limit").has("resource_limit"));
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.get(0).getResourceLimit(), Integer.valueOf(10));
        Assert.assertEquals(one.getProjectId(), PROJECT);
        Assert.assertNull(one.getDomainId());
    }
}
