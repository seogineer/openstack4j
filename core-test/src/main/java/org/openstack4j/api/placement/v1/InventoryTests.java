package org.openstack4j.api.placement.v1;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Inventories")
public class InventoryTests extends AbstractPlacementTest {

    public void listParsesAllResourceClasses() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/inventories.json");

        ResourceProviderInventories inventories = osv3().placement().inventories().list(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/inventories");
        Assert.assertEquals(inventories.getResourceProviderGeneration(), 557L);
        Assert.assertEquals(inventories.getInventories().keySet(), new java.util.HashSet<>(java.util.Arrays.asList("VCPU", "MEMORY_MB", "DISK_GB")));
        Inventory memory = inventories.getInventories().get("MEMORY_MB");
        Assert.assertEquals(memory.getTotal(), 15584L);
        Assert.assertEquals(memory.getReserved(), Long.valueOf(512));
        Assert.assertEquals(memory.getAllocationRatio(), Float.valueOf(1.0f));
    }

    public void getReturnsSingleInventory() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/inventory_vcpu.json");

        Inventory vcpu = osv3().placement().inventories().get(RP, "VCPU");

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/inventories/VCPU");
        Assert.assertEquals(vcpu.getTotal(), 12L);
        Assert.assertEquals(vcpu.getMaxUnit(), Long.valueOf(12));
        Assert.assertEquals(vcpu.getResourceProviderGeneration(), Long.valueOf(557));
    }

    public void getReturnsNullForUnknownResourceClass() throws Exception {
        respondWithVersions("1.39");
        respondWithError(404, "placement.undefined_code");

        Assert.assertNull(osv3().placement().inventories().get(RP, "CUSTOM_NOPE"));
        takeVersionAndRequest();
    }

    public void replaceSendsZeroGeneration() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/inventories.json");

        osv3().placement().inventories().replace(RP, 0L,
                Collections.singletonMap("VCPU", Inventory.builder().total(12).allocationRatio(4.0f).build()));
        RecordedRequest request = takeVersionAndRequest();
        JsonNode body = body(request);

        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(request.getPath(), "/resource_providers/" + RP + "/inventories");
        Assert.assertEquals(body.get("resource_provider_generation").asLong(), 0L);
        Assert.assertEquals(body.get("inventories").get("VCPU").get("total").asLong(), 12L);
        Assert.assertEquals(body.get("inventories").get("VCPU").get("allocation_ratio").asDouble(), 4.0, 0.0001);
        Assert.assertFalse(body.get("inventories").get("VCPU").has("reserved"), "unset fields must be omitted");
    }

    public void createPostsInventoryWithResourceClass() throws Exception {
        respondWithVersions("1.39");
        respondWith(201, "{\"total\": 8, \"reserved\": 0, \"min_unit\": 1, \"max_unit\": 8, \"step_size\": 1, \"allocation_ratio\": 1.0, \"resource_provider_generation\": 558}");

        Inventory created = osv3().placement().inventories().create(RP, 557L, "CUSTOM_GPU",
                Inventory.builder().total(8).maxUnit(8).build());
        RecordedRequest request = takeVersionAndRequest();
        JsonNode body = body(request);

        Assert.assertEquals(request.getMethod(), "POST");
        Assert.assertEquals(request.getPath(), "/resource_providers/" + RP + "/inventories");
        Assert.assertEquals(body.get("resource_class").asText(), "CUSTOM_GPU");
        Assert.assertEquals(body.get("resource_provider_generation").asLong(), 557L);
        Assert.assertEquals(body.get("total").asLong(), 8L);
        Assert.assertEquals(created.getResourceProviderGeneration(), Long.valueOf(558));
    }

    public void updatePutsSingleInventory() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/inventory_vcpu.json");

        osv3().placement().inventories().update(RP, 557L, "VCPU", Inventory.builder().total(12).reserved(2).build());
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(request.getPath(), "/resource_providers/" + RP + "/inventories/VCPU");
        Assert.assertEquals(body(request).get("reserved").asLong(), 2L);
        Assert.assertFalse(body(request).has("resource_class"));
    }

    public void deleteOneAndAll() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);
        respondWith(204);

        ActionResponse one = osv3().placement().inventories().delete(RP, "VCPU");
        ActionResponse all = osv3().placement().inventories().deleteAll(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/inventories/VCPU");
        Assert.assertEquals(takeRequest().getPath(), "/resource_providers/" + RP + "/inventories");
        Assert.assertTrue(one.isSuccess());
        Assert.assertTrue(all.isSuccess());
    }

    public void deleteWhileInUseIsReported() throws Exception {
        respondWithVersions("1.39");
        respondWithError(409, "placement.inventory.inuse");

        ActionResponse response = osv3().placement().inventories().delete(RP, "VCPU");

        takeVersionAndRequest();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getCode(), 409);
        Assert.assertTrue(response.getFault().contains("placement.inventory.inuse"), "fault must carry the Placement code: " + response.getFault());
    }
}
