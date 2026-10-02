package org.openstack4j.api.placement.v1;

import java.util.Arrays;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.placement.v1.ResourceProviderAggregates;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Aggregates")
public class AggregateTests extends AbstractPlacementTest {

    public void listForProviderParsesEmptyList() throws Exception {
        respondWithVersions("1.39");
        respondWith(200, "{\"aggregates\": [], \"resource_provider_generation\": 557}");

        ResourceProviderAggregates aggregates = osv3().placement().aggregates().listForProvider(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/aggregates");
        Assert.assertTrue(aggregates.getAggregates().isEmpty());
        Assert.assertEquals(aggregates.getResourceProviderGeneration(), 557L);
    }

    public void replaceForProviderSendsGenerationAndUuids() throws Exception {
        respondWithVersions("1.39");
        respondWith(200, "{\"aggregates\": [\"a1\", \"a2\"], \"resource_provider_generation\": 558}");

        ResourceProviderAggregates result = osv3().placement().aggregates().replaceForProvider(RP, 557L, Arrays.asList("a1", "a2"));
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(body(request).get("resource_provider_generation").asLong(), 557L);
        Assert.assertEquals(body(request).get("aggregates").size(), 2);
        Assert.assertEquals(result.getAggregates(), Arrays.asList("a1", "a2"));
        Assert.assertEquals(result.getResourceProviderGeneration(), 558L);
    }
}
