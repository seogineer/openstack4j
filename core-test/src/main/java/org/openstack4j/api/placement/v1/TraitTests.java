package org.openstack4j.api.placement.v1;

import java.util.Arrays;
import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceProviderTraits;
import org.openstack4j.model.placement.v1.TraitListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/Traits")
public class TraitTests extends AbstractPlacementTest {

    public void listWithFilters() throws Exception {
        respondWithVersions("1.39");
        respondWith(200, "{\"traits\": [\"HW_CPU_X86_AVX\", \"HW_CPU_X86_AVX2\"]}");

        List<String> traits = osv3().placement().traits().list(TraitListOptions.create().nameStartsWith("HW_CPU_X86_AVX").associated(true));
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getRequestUrl().encodedPath(), "/traits");
        Assert.assertEquals(request.getRequestUrl().queryParameter("name"), "startswith:HW_CPU_X86_AVX");
        Assert.assertEquals(request.getRequestUrl().queryParameter("associated"), "true");
        Assert.assertEquals(traits, Arrays.asList("HW_CPU_X86_AVX", "HW_CPU_X86_AVX2"));
    }

    public void existsUses204And404() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);
        respondWithError(404, "placement.undefined_code");

        Assert.assertTrue(osv3().placement().traits().exists("HW_CPU_X86_AVX"));
        Assert.assertFalse(osv3().placement().traits().exists("CUSTOM_NOPE"));
        Assert.assertEquals(takeVersionAndRequest().getPath(), "/traits/HW_CPU_X86_AVX");
        takeRequest();
    }

    public void createPutsTrait() throws Exception {
        respondWithVersions("1.39");
        respondWith(201);

        osv3().placement().traits().create("CUSTOM_GOLD");
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(request.getPath(), "/traits/CUSTOM_GOLD");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void createRequiresCustomPrefix() throws Exception {
        osv3().placement().traits().create("GOLD");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsInvalidName() throws Exception {
        osv3().placement().traits().exists("CUSTOM_A/B");
    }

    public void deleteTrait() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        ActionResponse response = osv3().placement().traits().delete("CUSTOM_GOLD");

        Assert.assertEquals(takeVersionAndRequest().getMethod(), "DELETE");
        Assert.assertTrue(response.isSuccess());
    }

    public void providerTraitsRoundTrip() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_traits.json");
        respondWith("/placement/v1/rp_traits.json");
        respondWith(204);

        ResourceProviderTraits current = osv3().placement().traits().listForProvider(RP);
        ResourceProviderTraits replaced = osv3().placement().traits().replaceForProvider(RP, current.getResourceProviderGeneration(),
                Arrays.asList("HW_CPU_X86_AVX2", "CUSTOM_GOLD"));
        ActionResponse cleared = osv3().placement().traits().deleteForProvider(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP + "/traits");
        RecordedRequest put = takeRequest();
        Assert.assertEquals(put.getMethod(), "PUT");
        Assert.assertEquals(body(put).get("resource_provider_generation").asLong(), 557L);
        Assert.assertEquals(body(put).get("traits").size(), 2);
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(current.getTraits().size(), 3);
        Assert.assertEquals(replaced.getResourceProviderGeneration(), 557L);
        Assert.assertTrue(cleared.isSuccess());
    }

    @Test(expectedExceptions = org.openstack4j.api.placement.v1.exceptions.PlacementException.class)
    public void existsThrowsOnServerErrorInsteadOfReturningFalse() throws Exception {
        respondWithVersions("1.39");
        respondWithError(500, "placement.undefined_code"); // not 503: HttpClient 5 retries 503 once by default
        try {
            osv3().placement().traits().exists("HW_CPU_X86_AVX");
        } finally {
            takeVersionAndRequest();
        }
    }
}
