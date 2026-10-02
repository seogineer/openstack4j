package org.openstack4j.api.compute.microversion;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ServerGroup;
import org.openstack4j.model.compute.actions.EvacuateRequest;
import org.openstack4j.model.compute.actions.LiveMigrateRequest;
import org.openstack4j.model.compute.ext.MigrationsFilter;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Legacy request shapes that must keep working once microversions are negotiated, found in the final review. */
@Test(suiteName = "Compute/ReviewFixes")
public class ReviewFixTests extends AbstractComputeMicroVersionTest {

    private static final String QUOTA = "{\"quota_set\": {\"id\": \"p1\", \"cores\": 20}}";

    public void quotaUpdateWithRemovedFieldsIsCapped() throws Exception {
        negotiate("2.100");
        respondWith(200, QUOTA);
        respondWith(200, QUOTA);
        respondWith(200, QUOTA);

        osv3().compute().quotaSets().updateForTenant("p1", Builders.quotaSet().floatingIps(10).build());
        osv3().compute().quotaSets().updateForTenant("p1", Builders.quotaSet().injectedFiles(5).build());
        osv3().compute().quotaSets().updateForTenant("p1", Builders.quotaSet().cores(20).build());

        assertVersionHeaders(takeRequest(), "2.35");
        assertVersionHeaders(takeRequest(), "2.56");
        assertVersionHeaders(takeRequest(), "2.100");
    }

    public void quotaClassUpdateWithRemovedFieldsIsCapped() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"quota_class_set\": {\"id\": \"default\", \"cores\": 20}}");
        osv3().compute().quotaSets().updateForClass("default", Builders.quotaSet().securityGroups(10).build());
        assertVersionHeaders(takeRequest(), "2.35");
    }

    public void legacyMigrationsFilterWithUnknownKeyIsCapped() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"migrations\": []}");
        respondWith(200, "{\"migrations\": []}");

        osv3().compute().migrations().list(MigrationsFilter.create().cellName("cell1"));
        osv3().compute().migrations().list(MigrationsFilter.create().host("compute-1"));

        assertVersionHeaders(takeRequest(), "2.58");
        assertVersionHeaders(takeRequest(), "2.100");
    }

    public void tenantUsageShowWithDetailedIsCapped() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"tenant_usage\": {\"tenant_id\": \"p1\"}}");
        osv3().compute().quotaSets().getTenantUsage("p1", "2026-09-01T00:00:00", "2026-10-01T00:00:00");
        assertVersionHeaders(takeRequest(), "2.74");
    }

    public void forceFalseIsNotSent() throws Exception {
        negotiate("2.100");
        respondWith(202);
        respondWith(200, "{}");
        respondWith(200, "{}");

        osv3().compute().servers().liveMigrate(SERVER, LiveMigrateRequest.create().host("compute-2").force(false));
        osv3().compute().servers().evacuate(SERVER, EvacuateRequest.create().host("compute-2").force(false));
        osv3().compute().servers().evacuate(SERVER, EvacuateRequest.create().onSharedStorage(true).force(false));

        RecordedRequest live = takeRequest();
        assertVersionHeaders(live, "2.100");
        Assert.assertFalse(body(live).get("os-migrateLive").has("force"));
        RecordedRequest evacuate = takeRequest();
        assertVersionHeaders(evacuate, "2.100");
        Assert.assertFalse(body(evacuate).get("evacuate").has("force"));
        RecordedRequest shared = takeRequest();
        assertVersionHeaders(shared, "2.13");
        JsonNode sharedBody = body(shared).get("evacuate");
        Assert.assertTrue(sharedBody.get("onSharedStorage").asBoolean());
        Assert.assertFalse(sharedBody.has("force"));
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.14.*")
    public void evacuateWithoutSharedStorageNeeds214() throws Exception {
        try {
            osv3().compute().servers().evacuate(SERVER, EvacuateRequest.create().host("compute-2"));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void serverGroupPoliciesFallBackToPolicy() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"server_group\": {\"id\": \"g1\", \"name\": \"web\", \"policy\": \"anti-affinity\", \"rules\": {}, \"members\": []}}");

        ServerGroup group = osv3().compute().serverGroups().get("g1");

        takeRequest();
        Assert.assertEquals(group.getPolicies(), Collections.singletonList("anti-affinity"));
    }
}
