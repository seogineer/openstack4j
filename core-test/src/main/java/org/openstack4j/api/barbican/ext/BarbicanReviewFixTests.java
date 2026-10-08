package org.openstack4j.api.barbican.ext;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Barbican/ReviewFixes")
public class BarbicanReviewFixTests extends AbstractBarbicanExtTest {

    public void refsAreAcceptedAsIds() throws Exception {
        respondWith(200, "{\"order_ref\": \"http://127.0.0.1:9311/v1/orders/o1\", \"status\": \"ACTIVE\"}");
        respondWith(java.util.Map.of("Content-Type", "text/plain"), 200, "beer");
        osv3().barbican().orders().get("https://kms.example.com:9311/v1/orders/o1");
        osv3().barbican().secretsExt().getTextPayload("http://kms.example.com/key-manager/v1/secrets/s1");
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v1/orders/o1"));
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v1/secrets/s1/payload"));
    }

    public void consumersAndProjectQuotasPage() throws Exception {
        respondWith(200, "{\"total\": 30, \"consumers\": []}");
        respondWith(200, "{\"total\": 30, \"consumers\": []}");
        respondWith(200, "{\"project_quotas\": [], \"total\": 30}");
        osv3().barbican().secretsExt().listConsumers("s1", Map.of("limit", "100", "offset", "10"));
        osv3().barbican().containersExt().listConsumers("c1", Map.of("limit", "100"));
        osv3().barbican().quotas().listProjectQuotas(Map.of("limit", "100"));
        String a = decodedPath(takeRequest());
        Assert.assertTrue(a.contains("limit=100") && a.contains("offset=10"), a);
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v1/containers/c1/consumers?limit=100"));
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v1/project-quotas?limit=100"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void aclWithNothingSetIsRejected() {
        osv3().barbican().acls().setSecretAcl("s1", null, null);
    }

    public void unnamedContainerSecret() throws Exception {
        respondWith(201, "{\"container_ref\": \"http://127.0.0.1:9311/v1/containers/c1\"}");
        Assert.assertTrue(osv3().barbican().containersExt().addSecret("c1", null, "http://127.0.0.1:9311/v1/secrets/s1").isSuccess());
        Assert.assertEquals(body(takeRequest()).toString(), "{\"secret_ref\":\"http://127.0.0.1:9311/v1/secrets/s1\"}");
    }
}
