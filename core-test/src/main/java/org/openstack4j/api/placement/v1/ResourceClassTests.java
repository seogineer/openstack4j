package org.openstack4j.api.placement.v1;

import java.util.Arrays;
import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceClasses;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/ResourceClasses")
public class ResourceClassTests extends AbstractPlacementTest {

    public void listReturnsNames() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/resource_classes.json");

        List<String> names = osv3().placement().resourceClasses().list();

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_classes");
        Assert.assertEquals(names, Arrays.asList("VCPU", "MEMORY_MB", "DISK_GB", "CUSTOM_GPU"));
        Assert.assertTrue(names.contains(ResourceClasses.VCPU));
    }

    public void existsIsTrueOn200AndFalseOn404() throws Exception {
        respondWithVersions("1.39");
        respondWith(200, "{\"name\": \"CUSTOM_GPU\", \"links\": []}");
        respondWithError(404, "placement.undefined_code");

        boolean gpu = osv3().placement().resourceClasses().exists("CUSTOM_GPU");
        boolean nope = osv3().placement().resourceClasses().exists("CUSTOM_NOPE");

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_classes/CUSTOM_GPU");
        takeRequest();
        Assert.assertTrue(gpu);
        Assert.assertFalse(nope);
    }

    public void createPostsName() throws Exception {
        respondWithVersions("1.39");
        respondWith(201);

        osv3().placement().resourceClasses().create("CUSTOM_GPU");
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "POST");
        Assert.assertEquals(request.getPath(), "/resource_classes");
        Assert.assertEquals(body(request).get("name").asText(), "CUSTOM_GPU");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createRequiresCustomPrefix() throws Exception {
        osv3().placement().resourceClasses().create("GPU");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsInvalidName() throws Exception {
        osv3().placement().resourceClasses().exists("CUSTOM_A/B");
    }

    public void ensurePutsNameAndAcceptsBothStatuses() throws Exception {
        respondWithVersions("1.39");
        respondWith(201);
        respondWith(204);

        osv3().placement().resourceClasses().ensure("CUSTOM_GPU");
        osv3().placement().resourceClasses().ensure("CUSTOM_GPU");

        RecordedRequest first = takeVersionAndRequest();
        Assert.assertEquals(first.getMethod(), "PUT");
        Assert.assertEquals(first.getPath(), "/resource_classes/CUSTOM_GPU");
        Assert.assertEquals(takeRequest().getMethod(), "PUT");
    }

    public void deleteReturnsActionResponse() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        ActionResponse response = osv3().placement().resourceClasses().delete("CUSTOM_GPU");

        Assert.assertEquals(takeVersionAndRequest().getMethod(), "DELETE");
        Assert.assertTrue(response.isSuccess());
    }
}
