package org.openstack4j.api.ext.small;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.AbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Magnum/Ext")
public class MagnumExtTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.MAGNUM;
    }

    private static String path(RecordedRequest r) {
        return URLDecoder.decode(r.getPath(), StandardCharsets.UTF_8);
    }

    public void resizeUpgradeCertificatesQuotasStats() throws Exception {
        String quota = "{\"resource\": \"Cluster\", \"hard_limit\": 10, \"project_id\": \"p1\", \"id\": 26}";
        respondWith(202, "{\"uuid\": \"c1\"}");
        respondWith(202, "{\"uuid\": \"c1\"}");
        respondWith(200, "{\"cluster_uuid\": \"c1\", \"pem\": \"-----BEGIN CERTIFICATE-----\"}");
        respondWith(201, quota);
        respondWith(200, "{\"quotas\": [" + quota + "]}");
        respondWith(200, quota);
        respondWith(202, quota);
        respondWith(204);
        respondWith(200, "{\"clusters\": 1, \"nodes\": 2}");

        var magnum = osv3().magnum().extensions();
        Assert.assertEquals(magnum.resizeCluster("c1", 3, List.of("n9"), "production_group"), "c1");
        Assert.assertEquals(magnum.upgradeCluster("c1", "tmpl2", 1, null), "c1");
        Map<String, Object> ca = magnum.getCaCertificate("c1", "etcd");
        Map<String, Object> created = magnum.createQuota("p1", "Cluster", 10);
        List<Map<String, Object>> quotas = magnum.listQuotas(Map.of("all_tenants", "True"));
        magnum.getQuota("p1", "Cluster");
        magnum.updateQuota("p1", "Cluster", 20);
        Assert.assertTrue(magnum.deleteQuota("p1", "Cluster").isSuccess());
        Map<String, Object> stats = magnum.stats("p1");

        RecordedRequest resize = takeRequest();
        Assert.assertTrue(path(resize).endsWith("/v1/clusters/c1/actions/resize"));
        Assert.assertEquals(resize.getHeader("OpenStack-API-Version"), "container-infra 1.7");
        Assert.assertEquals(new ObjectMapper().readTree(resize.getBody().readUtf8()).toString(),
                "{\"node_count\":3,\"nodes_to_remove\":[\"n9\"],\"nodegroup\":\"production_group\"}");
        RecordedRequest upgrade = takeRequest();
        Assert.assertEquals(upgrade.getHeader("OpenStack-API-Version"), "container-infra 1.8");
        Assert.assertEquals(new ObjectMapper().readTree(upgrade.getBody().readUtf8()).toString(), "{\"cluster_template\":\"tmpl2\",\"max_batch_size\":1}");
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/certificates/c1?ca_cert_type=etcd"));
        Assert.assertEquals(new ObjectMapper().readTree(takeRequest().getBody().readUtf8()).toString(), "{\"project_id\":\"p1\",\"resource\":\"Cluster\",\"hard_limit\":10}");
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/quotas?all_tenants=True"));
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/quotas/p1/Cluster"));
        RecordedRequest patch = takeRequest();
        Assert.assertEquals(patch.getMethod(), "PATCH");
        Assert.assertEquals(new ObjectMapper().readTree(patch.getBody().readUtf8()).toString(), "{\"project_id\":\"p1\",\"resource\":\"Cluster\",\"hard_limit\":20}");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertTrue(path(takeRequest()).endsWith("/v1/stats?project_id=p1"));
        Assert.assertEquals(ca.get("pem"), "-----BEGIN CERTIFICATE-----");
        Assert.assertEquals(created.get("hard_limit"), 10);
        Assert.assertEquals(quotas.get(0).get("resource"), "Cluster");
        Assert.assertEquals(stats.get("nodes"), 2);
    }
}
