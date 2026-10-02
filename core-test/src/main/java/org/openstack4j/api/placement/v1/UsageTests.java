package org.openstack4j.api.placement.v1;

import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.ProjectUsages;
import org.openstack4j.model.placement.v1.ResourceCapacity;
import org.openstack4j.model.placement.v1.ResourceProviderUsages;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Usages")
public class UsageTests extends AbstractPlacementTest {

    public void forProviderParsesUsages() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_usages.json");

        ResourceProviderUsages usages = osv3().placement().usages().forProvider(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/usages");
        Assert.assertEquals(usages.getResourceProviderGeneration(), 557L);
        Assert.assertEquals(usages.getUsages().get("MEMORY_MB"), Long.valueOf(1024));
        Assert.assertEquals(usages.getUsages().get("DISK_GB"), Long.valueOf(0));
    }

    public void forProjectGroupsByConsumerTypeOn138() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/usages_project.json");

        ProjectUsages usages = osv3().placement().usages().forProject("proj-1", "user-1", "INSTANCE");
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getRequestUrl().encodedPath(), "/usages");
        Assert.assertEquals(request.getRequestUrl().queryParameter("project_id"), "proj-1");
        Assert.assertEquals(request.getRequestUrl().queryParameter("user_id"), "user-1");
        Assert.assertEquals(request.getRequestUrl().queryParameter("consumer_type"), "INSTANCE");
        Assert.assertEquals(usages.getUsages().get("VCPU"), Long.valueOf(5));
        Assert.assertEquals(usages.getUsages().get("MEMORY_MB"), Long.valueOf(5120));
        Assert.assertEquals(usages.getByConsumerType().get("INSTANCE").getConsumerCount(), Long.valueOf(2));
        Assert.assertEquals(usages.getByConsumerType().get("INSTANCE").getUsages().get("VCPU"), Long.valueOf(4));
        Assert.assertEquals(usages.getByConsumerType().get("unknown").getUsages().get("VCPU"), Long.valueOf(1));
    }

    public void projectUsagesBefore138() throws Exception {
        respondWithVersions("1.37");
        respondWith(200, "{\"usages\": {\"VCPU\": 3, \"MEMORY_MB\": 2048}}");

        ProjectUsages usages = osv3().placement().usages().forProject("proj-1", null, null);
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getHeader("OpenStack-API-Version"), "placement 1.37");
        Assert.assertNull(request.getRequestUrl().queryParameter("user_id"));
        Assert.assertEquals(usages.getUsages().get("VCPU"), Long.valueOf(3));
        Assert.assertEquals(usages.getByConsumerType().size(), 1);
        Assert.assertNull(usages.getByConsumerType().get(ProjectUsages.ALL_CONSUMER_TYPES).getConsumerCount());
        Assert.assertEquals(usages.getByConsumerType().get(ProjectUsages.ALL_CONSUMER_TYPES).getUsages().get("MEMORY_MB"), Long.valueOf(2048));
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.38.*")
    public void consumerTypeFilterRequires138() throws Exception {
        respondWithVersions("1.37");
        try {
            osv3().placement().usages().forProject("proj-1", null, "INSTANCE");
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    public void capacityCombinesInventoriesAndUsages() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/inventories.json");
        respondWith("/placement/v1/rp_usages.json");

        Map<String, ResourceCapacity> capacity = osv3().placement().usages().capacity(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/inventories");
        Assert.assertEquals(takeRequest().getPath(), "/resource_providers/" + RP + "/usages");
        ResourceCapacity vcpu = capacity.get("VCPU");
        Assert.assertEquals(vcpu.getCapacity(), 48L);   // (12 - 0) * 4.0
        Assert.assertEquals(vcpu.getUsed(), 1L);
        Assert.assertEquals(vcpu.getFree(), 47L);
        ResourceCapacity memory = capacity.get("MEMORY_MB");
        Assert.assertEquals(memory.getCapacity(), 15072L); // (15584 - 512) * 1.0
        Assert.assertEquals(memory.getFree(), 14048L);
        Assert.assertEquals(capacity.get("DISK_GB").getUsed(), 0L);
    }
}
