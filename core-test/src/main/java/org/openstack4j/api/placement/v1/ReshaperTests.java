package org.openstack4j.api.placement.v1;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ReshapeRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Reshaper")
public class ReshaperTests extends AbstractPlacementTest {

    private static final String CHILD = "7b0c4d61-1d6b-4a3e-9d3b-8c1f2e5a9b11";
    private static final String CONSUMER = "96a38bed-26b5-410b-8cef-1913a2e0e0b8";

    private static ReshapeRequest request() {
        return ReshapeRequest.builder()
                .inventories(RP, 557L, Collections.singletonMap("VCPU", Inventory.builder().total(12).build()))
                .inventories(CHILD, 3L, Collections.singletonMap("VGPU", Inventory.builder().total(2).build()))
                .allocation(CONSUMER, AllocationRequest.builder().projectId("p").userId("u").consumerGeneration(1L).consumerType("INSTANCE")
                        .allocation(RP, "VCPU", 1).allocation(CHILD, "VGPU", 1).build())
                .build();
    }

    public void reshapePostsInventoriesAndAllocations() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        osv3().placement().reshaper().reshape(request());
        RecordedRequest post = takeVersionAndRequest();
        JsonNode body = body(post);

        Assert.assertEquals(post.getMethod(), "POST");
        Assert.assertEquals(post.getPath(), "/reshaper");
        Assert.assertEquals(body.get("inventories").get(RP).get("resource_provider_generation").asLong(), 557L);
        Assert.assertEquals(body.get("inventories").get(CHILD).get("inventories").get("VGPU").get("total").asLong(), 2L);
        Assert.assertEquals(body.get("allocations").get(CONSUMER).get("allocations").get(CHILD).get("resources").get("VGPU").asLong(), 1L);
        Assert.assertEquals(body.get("allocations").get(CONSUMER).get("consumer_generation").asLong(), 1L);
        Assert.assertEquals(body.get("allocations").get(CONSUMER).get("consumer_type").asText(), "INSTANCE");
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.30.*")
    public void reshapeRequires130() throws Exception {
        respondWithVersions("1.29");
        try {
            osv3().placement().reshaper().reshape(request());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }
}
