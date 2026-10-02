package org.openstack4j.api.placement.v1;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.placement.PlacementService;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.placement.v1.AllocationCandidates;
import org.openstack4j.model.placement.v1.AllocationCandidatesQuery;
import org.openstack4j.model.placement.v1.AllocationRequest;
import org.openstack4j.model.placement.v1.ConsumerAllocations;
import org.openstack4j.model.placement.v1.Inventory;
import org.openstack4j.model.placement.v1.PlacementVersion;
import org.openstack4j.model.placement.v1.ResourceCapacity;
import org.openstack4j.model.placement.v1.ResourceProvider;
import org.openstack4j.model.placement.v1.ResourceProviderCreate;
import org.openstack4j.model.placement.v1.ResourceProviderInventories;
import org.openstack4j.model.placement.v1.ResourceProviderListOptions;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Runs against a real OpenStack when the standard OS_* environment variables are set; skipped otherwise.
 * Creates a throw-away provider, resource class, trait and consumer and deletes them in {@code finally}.
 */
@Test(suiteName = "Placement/Live", groups = "placement-live")
public class PlacementLiveTests {

    private PlacementService placement;
    private String projectId;
    private String userId;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live Placement tests");
        String domain = env("OS_USER_DOMAIN_NAME", "Default");
        OSClientV3 os = OSFactory.builderV3()
                .endpoint(url.replaceAll("/+$", "").endsWith("/v3") ? url : url.replaceAll("/+$", "") + "/v3")
                .credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null), Identifier.byName(domain))
                .scopeToProject(Identifier.byName(env("OS_PROJECT_NAME", null)), Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default")))
                .authenticate();
        placement = os.placement();
        projectId = os.getToken().getProject().getId();
        userId = os.getToken().getUser().getId();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live Placement tests");
        }
        return value;
    }

    public void negotiatesAVersion() {
        PlacementVersion version = placement.versions().get();
        Assert.assertNotNull(version.getMicroVersion(), "server too old: " + version.getServerMaxVersion());
        Assert.assertFalse(version.isPinned());
    }

    public void readsExistingProvidersWithoutChangingThem() {
        for (ResourceProvider rp : placement.providers().list()) {
            Assert.assertNotNull(placement.providers().get(rp.getUuid()));
            placement.inventories().list(rp.getUuid());
            placement.usages().forProvider(rp.getUuid());
            placement.traits().listForProvider(rp.getUuid());
            placement.aggregates().listForProvider(rp.getUuid());
            placement.allocations().listForProvider(rp.getUuid());
            Map<String, ResourceCapacity> capacity = placement.usages().capacity(rp.getUuid());
            for (ResourceCapacity c : capacity.values())
                Assert.assertEquals(c.getFree(), c.getCapacity() - c.getUsed());
        }
        placement.usages().forProject(projectId, null, null);
        Assert.assertTrue(placement.resourceClasses().exists("VCPU"));
        Assert.assertFalse(placement.traits().list().isEmpty());
    }

    public void fullWriteRoundTripOnThrowAwayResources() {
        String tag = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String rcName = "CUSTOM_OS4J_IT_" + tag;
        String traitName = "CUSTOM_OS4J_IT_" + tag;
        String aggregate = UUID.randomUUID().toString();
        String consumer = UUID.randomUUID().toString();
        String rp = null;
        try {
            placement.resourceClasses().create(rcName);
            placement.traits().create(traitName);
            Assert.assertTrue(placement.resourceClasses().exists(rcName));
            Assert.assertTrue(placement.traits().exists(traitName));

            ResourceProvider created = placement.providers().create(ResourceProviderCreate.builder().name("os4j-it-" + tag).build());
            rp = created.getUuid();
            Assert.assertEquals(placement.providers().get(rp).getName(), "os4j-it-" + tag);

            ResourceProviderInventories inv = placement.inventories().replace(rp, created.getGeneration(),
                    Collections.singletonMap(rcName, Inventory.builder().total(10).reserved(2).allocationRatio(1.0f).maxUnit(10).build()));
            Assert.assertEquals(inv.getInventories().get(rcName).getTotal(), 10L);
            long gen = inv.getResourceProviderGeneration();

            gen = placement.traits().replaceForProvider(rp, gen, Collections.singletonList(traitName)).getResourceProviderGeneration();
            gen = placement.aggregates().replaceForProvider(rp, gen, Collections.singletonList(aggregate)).getResourceProviderGeneration();

            Assert.assertEquals(placement.providers().list(ResourceProviderListOptions.create().memberOf(aggregate).required(traitName)).size(), 1);

            AllocationCandidates candidates = placement.allocationCandidates().list(AllocationCandidatesQuery.builder()
                    .resources(rcName, 3).required(traitName).memberOf(aggregate).build());
            Assert.assertEquals(candidates.getAllocationRequests().size(), 1);

            String consumerType = new org.openstack4j.openstack.internal.MicroVersion(placement.versions().get().getMicroVersion())
                    .compareTo(org.openstack4j.model.placement.v1.PlacementMicroVersions.V1_38) >= 0 ? "OS4J_IT" : null;
            placement.allocations().set(consumer, AllocationRequest.builder().projectId(projectId).userId(userId)
                    .consumerType(consumerType).allocation(rp, rcName, 3).build());
            ConsumerAllocations allocations = placement.allocations().get(consumer);
            Assert.assertEquals(allocations.getAllocations().get(rp).getResources().get(rcName), Long.valueOf(3));
            Assert.assertEquals(placement.usages().capacity(rp).get(rcName).getFree(), 5L); // (10 - 2) * 1.0 - 3

            placement.allocations().set(consumer, AllocationRequest.builder().projectId(projectId).userId(userId)
                    .consumerType(consumerType).consumerGeneration(allocations.getConsumerGeneration()).allocation(rp, rcName, 4).build());
            Assert.assertEquals(placement.usages().forProvider(rp).getUsages().get(rcName), Long.valueOf(4));
        } finally {
            placement.allocations().delete(consumer);
            if (rp != null) {
                placement.inventories().deleteAll(rp);
                placement.traits().deleteForProvider(rp);
                placement.providers().delete(rp);
            }
            placement.traits().delete(traitName);
            placement.resourceClasses().delete(rcName);
        }
        Assert.assertNull(placement.providers().get(rp));
        Assert.assertFalse(placement.resourceClasses().exists(rcName));
        Assert.assertFalse(placement.traits().exists(traitName));
    }
}
