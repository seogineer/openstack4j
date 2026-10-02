package org.openstack4j.api.compute.microversion;

import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.KeypairListOptions;
import org.openstack4j.model.compute.QuotaSet;
import org.openstack4j.model.compute.ServerGroup;
import org.openstack4j.model.compute.ext.HypervisorListOptions;
import org.openstack4j.model.compute.ext.ServiceUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Compute/ServiceExtensions")
public class ComputeServiceExtensionsTests extends AbstractComputeMicroVersionTest {

    private static final String SERVICE_ID = "e81d66a4-ddd3-4aba-8a84-171d1cb4d339";

    public void hypervisorListOptions() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"hypervisors\": [{\"id\": \"c48f6247-abe4-4a24-824e-ea39e108874f\", \"hypervisor_hostname\": \"compute-1\", \"state\": \"up\", \"status\": \"enabled\"}]}");

        osv3().compute().hypervisors().list(HypervisorListOptions.create().hypervisorHostnamePattern("compute").withServers(true).limit(5));

        String path = takeRequest().getPath();
        Assert.assertTrue(path.contains("/os-hypervisors/detail?"), path);
        Assert.assertTrue(path.contains("hypervisor_hostname_pattern=compute"), path);
        Assert.assertTrue(path.contains("with_servers=true"), path);
        Assert.assertTrue(path.contains("limit=5"), path);
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.53.*")
    public void hypervisorPatternNeeds253() throws Exception {
        try {
            osv3().compute().hypervisors().list(HypervisorListOptions.create().hypervisorHostnamePattern("c"));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void serviceUpdateDeleteAndDisableWithReason() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"service\": {\"id\": \"" + SERVICE_ID + "\", \"binary\": \"nova-compute\", \"host\": \"compute-1\", \"status\": \"disabled\", \"disabled_reason\": \"maint\", \"forced_down\": false}}");
        respondWith(204);
        respondWith(200, "{\"service\": {\"binary\": \"nova-compute\", \"host\": \"compute-1\", \"status\": \"disabled\", \"disabled_reason\": \"maint\"}}");

        org.openstack4j.model.compute.ext.Service updated = osv3().compute().services().update(SERVICE_ID, ServiceUpdate.create().disable("maint"));
        boolean deleted = osv3().compute().services().delete(SERVICE_ID).isSuccess();
        osv3().compute().services().disableWithReason("nova-compute", "compute-1", "maint");

        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        Assert.assertTrue(update.getPath().endsWith("/os-services/" + SERVICE_ID));
        Assert.assertEquals(body(update).get("status").asText(), "disabled");
        Assert.assertEquals(body(update).get("disabled_reason").asText(), "maint");
        Assert.assertFalse(body(update).has("forced_down"));
        RecordedRequest delete = takeRequest();
        Assert.assertEquals(delete.getMethod(), "DELETE");
        RecordedRequest reason = takeRequest();
        Assert.assertTrue(reason.getPath().endsWith("/os-services/disable-log-reason"));
        assertVersionHeaders(reason, "2.52");
        Assert.assertEquals(body(reason).get("disabled_reason").asText(), "maint");
        Assert.assertEquals(updated.getDisabledReason(), "maint");
        Assert.assertTrue(deleted);
    }

    public void aggregateCacheImagesAndQuotaDefaults() throws Exception {
        negotiate("2.100");
        respondWith(202);
        respondWith(200, "{\"quota_set\": {\"id\": \"p1\", \"cores\": 20, \"instances\": 10, \"ram\": 51200}}");

        Assert.assertTrue(osv3().compute().hostAggregates().cacheImages("1", Arrays.asList("img-1", "img-2")).isSuccess());
        QuotaSet defaults = osv3().compute().quotaSets().defaults("p1");

        RecordedRequest cache = takeRequest();
        Assert.assertTrue(cache.getPath().endsWith("/os-aggregates/1/images"));
        JsonNode list = body(cache).get("cache");
        Assert.assertEquals(list.get(1).get("id").asText(), "img-2");
        Assert.assertTrue(takeRequest().getPath().endsWith("/os-quota-sets/p1/defaults"));
        Assert.assertEquals(defaults.getCores(), 20);
    }

    public void keypairListOptionsAndTypedCreate() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"keypairs\": [{\"keypair\": {\"name\": \"k1\", \"type\": \"ssh\", \"public_key\": \"ssh-rsa A\", \"fingerprint\": \"f\"}}]}");
        respondWith(201, "{\"keypair\": {\"name\": \"x\", \"type\": \"x509\", \"public_key\": \"-----BEGIN CERTIFICATE-----\", \"fingerprint\": \"f\", \"user_id\": \"u1\"}}");

        osv3().compute().keypairs().list(KeypairListOptions.create().userId("u1").limit(10).marker("k0"));
        Keypair created = osv3().compute().keypairs().create("x", "-----BEGIN CERTIFICATE-----", "x509");

        String path = takeRequest().getPath();
        Assert.assertTrue(path.contains("user_id=u1") && path.contains("limit=10") && path.contains("marker=k0"), path);
        RecordedRequest create = takeRequest();
        Assert.assertEquals(body(create).get("keypair").get("type").asText(), "x509");
        assertVersionHeaders(create, "2.100");
        Assert.assertEquals(created.getType(), "x509");
    }

    public void serverGroupWithPolicyAndRules() throws Exception {
        negotiate("2.100");
        respondWith(200, "{\"server_group\": {\"id\": \"g1\", \"name\": \"web\", \"policy\": \"anti-affinity\", \"rules\": {\"max_server_per_host\": 3},"
                + " \"members\": [], \"project_id\": \"p1\", \"user_id\": \"u1\"}}");

        ServerGroup group = osv3().compute().serverGroups().create("web", "anti-affinity", Collections.singletonMap("max_server_per_host", 3));

        JsonNode sent = body(takeRequest()).get("server_group");
        Assert.assertEquals(sent.get("policy").asText(), "anti-affinity");
        Assert.assertEquals(sent.get("rules").get("max_server_per_host").asInt(), 3);
        Assert.assertFalse(sent.has("policies"));
        Assert.assertEquals(group.getPolicy(), "anti-affinity");
        Assert.assertEquals(((Number) group.getRules().get("max_server_per_host")).intValue(), 3);
        Assert.assertEquals(group.getProjectId(), "p1");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*2\\.64.*")
    public void serverGroupPolicyNeeds264() throws Exception {
        try {
            osv3().compute().serverGroups().create("web", "affinity", null);
        } finally {
            assertNoMoreRequests();
        }
    }
}
