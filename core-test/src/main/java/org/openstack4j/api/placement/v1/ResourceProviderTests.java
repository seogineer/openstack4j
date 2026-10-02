package org.openstack4j.api.placement.v1;

import java.util.List;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementConcurrentUpdateException;
import org.openstack4j.api.placement.v1.exceptions.PlacementException;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.placement.v1.ResourceProvider;
import org.openstack4j.model.placement.v1.ResourceProviderCreate;
import org.openstack4j.model.placement.v1.ResourceProviderListOptions;
import org.openstack4j.model.placement.v1.ResourceProviderUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/ResourceProviders")
public class ResourceProviderTests extends AbstractPlacementTest {

    private static final String CHILD = "7b0c4d61-1d6b-4a3e-9d3b-8c1f2e5a9b11";

    public void listSendsVersionHeaderAndParsesTree() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_list.json");

        List<? extends ResourceProvider> providers = osv3().placement().providers().list();
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "GET");
        Assert.assertEquals(request.getPath(), "/resource_providers");
        Assert.assertEquals(request.getHeader("OpenStack-API-Version"), "placement 1.39");
        Assert.assertEquals(providers.size(), 2);
        Assert.assertEquals(providers.get(0).getName(), "compute-1");
        Assert.assertEquals(providers.get(0).getGeneration(), Long.valueOf(557));
        Assert.assertNull(providers.get(0).getParentProviderUuid());
        Assert.assertEquals(providers.get(1).getParentProviderUuid(), RP);
        Assert.assertEquals(providers.get(1).getRootProviderUuid(), RP);
    }

    public void listOptionsBecomeQueryParameters() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_list.json");

        osv3().placement().providers().list(ResourceProviderListOptions.create()
                .name("compute-1")
                .resources("VCPU", 2).resources("MEMORY_MB", 1024)
                .memberOf("agg-1")
                .inTree(RP)
                .required("HW_CPU_X86_AVX2", "!CUSTOM_SLOW"));
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getRequestUrl().queryParameter("name"), "compute-1");
        Assert.assertEquals(request.getRequestUrl().queryParameter("resources"), "VCPU:2,MEMORY_MB:1024");
        Assert.assertEquals(request.getRequestUrl().queryParameter("member_of"), "agg-1");
        Assert.assertEquals(request.getRequestUrl().queryParameter("in_tree"), RP);
        Assert.assertEquals(request.getRequestUrl().queryParameter("required"), "HW_CPU_X86_AVX2,!CUSTOM_SLOW");
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.32.*")
    public void forbiddenAggregateRequires132() throws Exception {
        respondWithVersions("1.31");
        try {
            osv3().placement().providers().list(ResourceProviderListOptions.create().memberOf("!agg-1"));
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.39.*")
    public void traitInSyntaxRequires139() throws Exception {
        respondWithVersions("1.38");
        try {
            osv3().placement().providers().list(ResourceProviderListOptions.create().required("in:A,B"));
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    public void getReturnsProvider() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_get.json");

        ResourceProvider provider = osv3().placement().providers().get(RP);

        Assert.assertEquals(takeVersionAndRequest().getPath(), "/resource_providers/" + RP);
        Assert.assertEquals(provider.getUuid(), RP);
        Assert.assertEquals(provider.getGeneration(), Long.valueOf(557));
    }

    public void getReturnsNullOn404() throws Exception {
        respondWithVersions("1.39");
        respondWithError(404, "placement.undefined_code");

        Assert.assertNull(osv3().placement().providers().get("00000000-0000-0000-0000-000000000000"));
        takeVersionAndRequest();
    }

    public void createSendsBodyAndReturnsProvider() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_get.json");

        ResourceProvider created = osv3().placement().providers().create(
                ResourceProviderCreate.builder().name("compute-1").uuid(RP).build());
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "POST");
        Assert.assertEquals(request.getPath(), "/resource_providers");
        Assert.assertEquals(body(request).get("name").asText(), "compute-1");
        Assert.assertEquals(body(request).get("uuid").asText(), RP);
        Assert.assertFalse(body(request).has("parent_provider_uuid"));
        Assert.assertEquals(created.getUuid(), RP);
    }

    public void updateRenamesProvider() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_get.json");

        osv3().placement().providers().update(CHILD, ResourceProviderUpdate.builder().name("compute-1").build());
        RecordedRequest request = takeVersionAndRequest();

        Assert.assertEquals(request.getMethod(), "PUT");
        Assert.assertEquals(request.getPath(), "/resource_providers/" + CHILD);
        Assert.assertEquals(body(request).get("name").asText(), "compute-1");
        Assert.assertFalse(body(request).has("parent_provider_uuid"));
    }

    public void unparentSendsExplicitNull() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/rp_get.json");

        osv3().placement().providers().update(CHILD, ResourceProviderUpdate.builder().name("x").unparent().build());

        Assert.assertTrue(body(takeVersionAndRequest()).get("parent_provider_uuid").isNull());
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.37.*")
    public void unparentRequires137() throws Exception {
        respondWithVersions("1.36");
        try {
            osv3().placement().providers().update(CHILD, ResourceProviderUpdate.builder().name("x").unparent().build());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    public void deleteReturnsActionResponse() throws Exception {
        respondWithVersions("1.39");
        respondWith(204);

        ActionResponse response = osv3().placement().providers().delete(CHILD);

        Assert.assertEquals(takeVersionAndRequest().getMethod(), "DELETE");
        Assert.assertTrue(response.isSuccess());
    }

    public void deleteConflictIsReportedInActionResponse() throws Exception {
        respondWithVersions("1.39");
        respondWithError(409, "placement.resource_provider.inuse");

        ActionResponse response = osv3().placement().providers().delete(RP);

        takeVersionAndRequest();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getCode(), 409);
    }

    public void concurrentUpdateIsMappedToItsOwnException() throws Exception {
        respondWithVersions("1.39");
        respondWithError(409, "placement.concurrent_update");
        try {
            osv3().placement().providers().update(RP, ResourceProviderUpdate.builder().name("x").build());
            Assert.fail("expected PlacementConcurrentUpdateException");
        } catch (PlacementConcurrentUpdateException e) {
            Assert.assertEquals(e.getErrorCode(), "placement.concurrent_update");
            Assert.assertEquals(e.getStatus(), 409);
            Assert.assertEquals(e.getRequestId(), "req-test");
        }
        takeVersionAndRequest();
    }

    public void otherPlacementErrorsKeepTheirCode() throws Exception {
        respondWithVersions("1.39");
        respondWithError(400, "placement.duplicate_name");
        try {
            osv3().placement().providers().create(ResourceProviderCreate.builder().name("dup").build());
            Assert.fail("expected PlacementException");
        } catch (PlacementConcurrentUpdateException e) {
            Assert.fail("must not be a concurrent update");
        } catch (PlacementException e) {
            Assert.assertEquals(e.getErrorCode(), "placement.duplicate_name");
            Assert.assertEquals(e.getDetail(), "detail for placement.duplicate_name");
        }
        takeVersionAndRequest();
    }

    public void nonPlacementErrorBodyStillMapsStatus() throws Exception {
        respondWithVersions("1.39");
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Content-Type", "text/html");
        respondWith(headers, 502, "<html><body>Bad Gateway</body></html>");
        try {
            osv3().placement().providers().get(RP);
            Assert.fail("expected PlacementException");
        } catch (PlacementException e) {
            Assert.assertEquals(e.getStatus(), 502);
            Assert.assertNull(e.getErrorCode());
        }
        takeVersionAndRequest();
    }
}
