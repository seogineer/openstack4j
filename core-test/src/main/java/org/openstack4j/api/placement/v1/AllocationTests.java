package org.openstack4j.api.placement.v1;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.ConsumerAllocations;
import org.openstack4j.model.placement.v1.ResourceProviderAllocations;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Allocations")
public class AllocationTests extends AbstractPlacementTest {

    private static final String CONSUMER = "96a38bed-26b5-410b-8cef-1913a2e0e0b8";

    private static AllocationRequest.Builder request() {
        return AllocationRequest.builder().projectId("proj-1").userId("user-1").allocation(RP, "VCPU", 2).allocation(RP, "MEMORY_MB", 2048);
    }

    public void getParsesConsumerAllocations() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/consumer_allocations.json");

        ConsumerAllocations allocations = osv3().placement().allocations().get(CONSUMER);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/allocations/" + CONSUMER);
        Assert.assertEquals(allocations.getAllocations().get(RP).getResources().get("MEMORY_MB"), Long.valueOf(1024));
        Assert.assertEquals(allocations.getAllocations().get(RP).getGeneration(), Long.valueOf(557));
        Assert.assertEquals(allocations.getConsumerGeneration(), Long.valueOf(1));
        Assert.assertEquals(allocations.getConsumerType(), "INSTANCE");
        Assert.assertEquals(allocations.getProjectId(), "2580a7b51d564c1d848ee27fda2db713");
    }

    public void getOfUnknownConsumerIsEmpty() throws Exception {
        respondWithVersions("1.39");
        respondWith(200, "{\"allocations\": {}}");

        ConsumerAllocations allocations = osv3().placement().allocations().get("00000000-0000-0000-0000-000000000000");

        takeVersionAndRequest();
        Assert.assertTrue(allocations.getAllocations().isEmpty());
        Assert.assertNull(allocations.getConsumerGeneration());
    }

    public void setNewConsumerSendsNullGenerationAndType() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        osv3().placement().allocations().set(CONSUMER, request().consumerType("INSTANCE").build());
        RecordedRequest put = takeVersionAndRequest();
        JsonNode body = body(put);

        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertEquals(put.getPath(), "/allocations/" + CONSUMER);
        Assert.assertTrue(body.has("consumer_generation"), "consumer_generation must be present");
        Assert.assertTrue(body.get("consumer_generation").isNull());
        Assert.assertEquals(body.get("consumer_type").asText(), "INSTANCE");
        Assert.assertEquals(body.get("project_id").asText(), "proj-1");
        Assert.assertEquals(body.get("allocations").get(RP).get("resources").get("VCPU").asLong(), 2L);
    }

    public void setExistingConsumerSendsGeneration() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        osv3().placement().allocations().set(CONSUMER, request().consumerGeneration(1L).consumerType("INSTANCE").build());

        Assert.assertEquals(body(takeVersionAndRequest()).get("consumer_generation").asLong(), 1L);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*consumer type.*1\\.38.*")
    public void consumerTypeIsRequiredFrom138() throws Exception {
        respondWithVersions("1.39");
        try {
            osv3().placement().allocations().set(CONSUMER, request().build());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.38.*")
    public void consumerTypeIsRejectedBefore138() throws Exception {
        respondWithVersions("1.37");
        try {
            osv3().placement().allocations().set(CONSUMER, request().consumerType("INSTANCE").build());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    public void setBefore138OmitsConsumerType() throws Exception {
        respondWithVersions("1.37");
        respondWith(204);

        osv3().placement().allocations().set(CONSUMER, request().build());
        JsonNode body = body(takeVersionAndRequest());

        Assert.assertFalse(body.has("consumer_type"));
        Assert.assertTrue(body.get("consumer_generation").isNull());
    }

    public void setManyPostsAllConsumers() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        osv3().placement().allocations().setMany(Collections.singletonMap(CONSUMER, request().consumerType("INSTANCE").build()));
        RecordedRequest post = takeVersionAndRequest();

        Assert.assertEquals(post.getMethod(), "POST");
        Assert.assertEquals(post.getPath(), "/allocations");
        Assert.assertEquals(body(post).get(CONSUMER).get("allocations").get(RP).get("resources").get("MEMORY_MB").asLong(), 2048L);
        Assert.assertEquals(body(post).get(CONSUMER).get("consumer_type").asText(), "INSTANCE");
    }

    public void deleteConsumer() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        ActionResponse response = osv3().placement().allocations().delete(CONSUMER);

        Assert.assertEquals(takeVersionAndRequest().getMethod(), "DELETE");
        Assert.assertTrue(response.isSuccess());
    }

    public void listForProvider() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_allocations.json");

        ResourceProviderAllocations allocations = osv3().placement().allocations().listForProvider(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/allocations");
        Assert.assertEquals(allocations.getResourceProviderGeneration(), 557L);
        Assert.assertEquals(allocations.getAllocations().get(CONSUMER).getResources().get("VCPU"), Long.valueOf(1));
        Assert.assertEquals(allocations.getAllocations().get(CONSUMER).getConsumerGeneration(), Long.valueOf(1));
    }

    @Test(expectedExceptions = PlacementConcurrentUpdateException.class)
    public void staleConsumerGenerationIsAConcurrentUpdate() throws Exception {
        respondWithVersions("1.39");
        respondWithError(409, "placement.concurrent_update");
        try {
            osv3().placement().allocations().set(CONSUMER, request().consumerGeneration(1L).consumerType("INSTANCE").build());
        } finally {
            takeVersionAndRequest();
        }
    }
}
