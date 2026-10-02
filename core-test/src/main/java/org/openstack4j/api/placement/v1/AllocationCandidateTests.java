package org.openstack4j.api.placement.v1;

import java.util.Arrays;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.AllocationCandidates;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery.GroupPolicy;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Placement/AllocationCandidates")
public class AllocationCandidateTests extends AbstractPlacementTest {

    public void simpleQueryAndResponse() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/candidates.json");

        AllocationCandidates candidates = osv3().placement().allocationCandidates().list(AllocationCandidatesQuery.builder()
                .resources("VCPU", 1).resources("MEMORY_MB", 512).required("HW_CPU_X86_AVX2", "!CUSTOM_SLOW").limit(2).build());
        HttpUrl url = takeVersionAndRequest().getRequestUrl();

        Assert.assertEquals(url.encodedPath(), "/allocation_candidates");
        Assert.assertEquals(url.queryParameter("resources"), "VCPU:1,MEMORY_MB:512");
        Assert.assertEquals(url.queryParameter("required"), "HW_CPU_X86_AVX2,!CUSTOM_SLOW");
        Assert.assertEquals(url.queryParameter("limit"), "2");
        Assert.assertNull(url.queryParameter("group_policy"));
        Assert.assertEquals(candidates.getAllocationRequests().size(), 1);
        Assert.assertEquals(candidates.getAllocationRequests().get(0).getAllocations().get(RP).get("MEMORY_MB"), Long.valueOf(512));
        Assert.assertEquals(candidates.getAllocationRequests().get(0).getMappings().get(""), Arrays.asList(RP));
        Assert.assertEquals(candidates.getProviderSummaries().get(RP).getResources().get("VCPU").getCapacity(), 48L);
        Assert.assertEquals(candidates.getProviderSummaries().get(RP).getResources().get("VCPU").getUsed(), 1L);
        Assert.assertEquals(candidates.getProviderSummaries().get(RP).getTraits().size(), 2);
        Assert.assertNull(candidates.getProviderSummaries().get(RP).getParentProviderUuid());
    }

    public void granularGroupsGetSuffixedParameters() throws Exception {
        respondWithVersions("1.39");
        respondWith("/placement/v1/candidates.json");

        osv3().placement().allocationCandidates().list(AllocationCandidatesQuery.builder()
                .resources("VCPU", 2)
                .group("_GPU", g -> g.resources("VGPU", 1).required("CUSTOM_NVIDIA").memberOf("agg-gpu"))
                .group("_NET", g -> g.resources("NET_BW_EGR_KILOBIT_PER_SEC", 1000).inTree(RP))
                .groupPolicy(GroupPolicy.ISOLATE)
                .rootRequired("COMPUTE_STATUS_ENABLED")
                .sameSubtree("_GPU", "_NET")
                .build());
        HttpUrl url = takeVersionAndRequest().getRequestUrl();

        Assert.assertEquals(url.queryParameter("resources"), "VCPU:2");
        Assert.assertEquals(url.queryParameter("resources_GPU"), "VGPU:1");
        Assert.assertEquals(url.queryParameter("required_GPU"), "CUSTOM_NVIDIA");
        Assert.assertEquals(url.queryParameter("member_of_GPU"), "agg-gpu");
        Assert.assertEquals(url.queryParameter("in_tree_NET"), RP);
        Assert.assertEquals(url.queryParameter("group_policy"), "isolate");
        Assert.assertEquals(url.queryParameter("root_required"), "COMPUTE_STATUS_ENABLED");
        Assert.assertEquals(url.queryParameter("same_subtree"), "_GPU,_NET");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*group_policy.*")
    public void twoGroupsNeedAGroupPolicy() {
        AllocationCandidatesQuery.builder()
                .group("1", g -> g.resources("VCPU", 1))
                .group("2", g -> g.resources("MEMORY_MB", 1))
                .build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void atLeastOneResourceIsRequired() {
        AllocationCandidatesQuery.builder().required("HW_CPU_X86_AVX2").build();
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*same_subtree.*1\\.36.*")
    public void sameSubtreeRequires136() throws Exception {
        respondWithVersions("1.35");
        try {
            osv3().placement().allocationCandidates().list(AllocationCandidatesQuery.builder()
                    .group("1", g -> g.resources("VCPU", 1)).sameSubtree("1").build());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = PlacementMicroVersionException.class, expectedExceptionsMessageRegExp = ".*1\\.33.*")
    public void nonNumericSuffixRequires133() throws Exception {
        respondWithVersions("1.32");
        try {
            osv3().placement().allocationCandidates().list(AllocationCandidatesQuery.builder()
                    .group("_GPU", g -> g.resources("VGPU", 1)).build());
        } finally {
            takeRequest();
            assertNoMoreRequests();
        }
    }

    public void numericSuffixWorksBefore133() throws Exception {
        respondWithVersions("1.32");
        respondWith("/placement/v1/candidates.json");

        osv3().placement().allocationCandidates().list(AllocationCandidatesQuery.builder()
                .group("1", g -> g.resources("VGPU", 1)).build());

        Assert.assertEquals(takeVersionAndRequest().getRequestUrl().queryParameter("resources1"), "VGPU:1");
    }
}
