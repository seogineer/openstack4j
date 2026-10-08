package org.openstack4j.api.barbican.ext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.barbican.ext.BarbicanAcl;
import org.openstack4j.model.barbican.ext.Order;
import org.openstack4j.model.barbican.ext.SecretStore;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Barbican/Ext")
public class BarbicanExtTests extends AbstractBarbicanExtTest {

    private static final String SECRET_REF = "http://127.0.0.1:9311/v1/secrets/s1";

    public void acls() throws Exception {
        respondWith(200, "{\"read\": {\"updated\": \"2015-05-12T20:08:47.644264\", \"created\": \"2015-05-12T19:23:44.019168\", \"users\": [\"u1\", \"u2\"], \"project-access\": false}}");
        respondWith(200, "{\"acl_ref\": \"http://127.0.0.1:9311/v1/secrets/s1/acl\"}");
        respondWith(200, "{\"acl_ref\": \"http://127.0.0.1:9311/v1/secrets/s1/acl\"}");
        respondWith(200);
        respondWith(200, "{\"read\": {\"project-access\": true}}");
        respondWith(200, "{\"acl_ref\": \"http://127.0.0.1:9311/v1/containers/c1/acl\"}");

        var acls = osv3().barbican().acls();
        BarbicanAcl acl = acls.getSecretAcl("s1");
        Assert.assertTrue(acls.setSecretAcl("s1", List.of("u1"), false).isSuccess());
        Assert.assertTrue(acls.updateSecretAcl("s1", null, true).isSuccess());
        Assert.assertTrue(acls.deleteSecretAcl("s1").isSuccess());
        BarbicanAcl containerAcl = acls.getContainerAcl("c1");
        Assert.assertTrue(acls.setContainerAcl("c1", List.of(), true).isSuccess());

        expect("GET", "/v1/secrets/s1/acl");
        Assert.assertEquals(body(expect("PUT", "/v1/secrets/s1/acl")).toString(), "{\"read\":{\"users\":[\"u1\"],\"project-access\":false}}");
        Assert.assertEquals(body(expect("PATCH", "/v1/secrets/s1/acl")).toString(), "{\"read\":{\"project-access\":true}}");
        expect("DELETE", "/v1/secrets/s1/acl");
        expect("GET", "/v1/containers/c1/acl");
        Assert.assertEquals(body(expect("PUT", "/v1/containers/c1/acl")).toString(), "{\"read\":{\"users\":[],\"project-access\":true}}");
        Assert.assertEquals(acl.getUsers(), List.of("u1", "u2"));
        Assert.assertEquals(acl.isProjectAccess(), Boolean.FALSE);
        Assert.assertEquals(containerAcl.isProjectAccess(), Boolean.TRUE);
        Assert.assertNull(containerAcl.getUsers());
    }

    public void secretPayloadMetadataConsumers() throws Exception {
        respondWith(204);
        respondWith(204);
        respondWith(java.util.Map.of("Content-Type", "text/plain"), 200, "beer");
        respondWith(java.util.Map.of("Content-Type", "application/octet-stream"), 200, "\u0001\u0002");
        respondWith(200, "{\"metadata\": {\"description\": \"contains the AES key\"}}");
        respondWith(201, "{\"metadata_ref\": \"http://127.0.0.1:9311/v1/secrets/s1/metadata\"}");
        respondWith(201, "{\"key\": \"access-limit\", \"value\": \"11\"}");
        respondWith(200, "{\"key\": \"access-limit\", \"value\": \"11\"}");
        respondWith(200, "{\"key\": \"access-limit\", \"value\": \"12\"}");
        respondWith(204);
        respondWith(200, "{\"total\": 1, \"consumers\": [{\"service\": \"image\", \"resource_type\": \"image\", \"resource_id\": \"img1\", \"status\": \"ACTIVE\"}]}");
        respondWith(200, "{\"name\": \"secret name\", \"consumers\": [{\"service\": \"image\", \"resource_type\": \"image\", \"resource_id\": \"img1\"}]}");
        respondWith(200, "{\"name\": \"secret name\", \"consumers\": []}");

        var ext = osv3().barbican().secretsExt();
        Assert.assertTrue(ext.storeTextPayload("s1", "beer").isSuccess());
        Assert.assertTrue(ext.storeBinaryPayload("s1", new byte[] {1, 2}).isSuccess());
        String text = ext.getTextPayload("s1");
        byte[] binary = ext.getBinaryPayload("s1");
        Map<String, String> metadata = ext.getMetadata("s1");
        Assert.assertTrue(ext.replaceMetadata("s1", Map.of("description", "d")).isSuccess());
        Assert.assertTrue(ext.addMetadataItem("s1", "access-limit", "11").isSuccess());
        String item = ext.getMetadataItem("s1", "access-limit");
        Assert.assertTrue(ext.updateMetadataItem("s1", "access-limit", "12").isSuccess());
        Assert.assertTrue(ext.deleteMetadataItem("s1", "access-limit").isSuccess());
        List<Map<String, Object>> consumers = ext.listConsumers("s1");
        Assert.assertTrue(ext.registerConsumer("s1", "image", "image", "img1").isSuccess());
        Assert.assertTrue(ext.removeConsumer("s1", "image", "image", "img1").isSuccess());

        RecordedRequest text1 = expect("PUT", "/v1/secrets/s1");
        Assert.assertTrue(text1.getHeader("Content-Type").startsWith("text/plain"), text1.getHeader("Content-Type"));
        Assert.assertEquals(text1.getBody().readUtf8(), "beer");
        RecordedRequest bin = expect("PUT", "/v1/secrets/s1");
        Assert.assertTrue(bin.getHeader("Content-Type").startsWith("application/octet-stream"), bin.getHeader("Content-Type"));
        Assert.assertEquals(bin.getHeader("Content-Encoding"), "base64");
        Assert.assertEquals(bin.getBody().readUtf8(), Base64.getEncoder().encodeToString(new byte[] {1, 2}));
        Assert.assertEquals(expect("GET", "/v1/secrets/s1/payload").getHeader("Accept"), "text/plain");
        Assert.assertEquals(expect("GET", "/v1/secrets/s1/payload").getHeader("Accept"), "application/octet-stream");
        expect("GET", "/v1/secrets/s1/metadata");
        Assert.assertEquals(body(expect("PUT", "/v1/secrets/s1/metadata")).toString(), "{\"metadata\":{\"description\":\"d\"}}");
        Assert.assertEquals(body(expect("POST", "/v1/secrets/s1/metadata/")).toString(), "{\"key\":\"access-limit\",\"value\":\"11\"}");
        expect("GET", "/v1/secrets/s1/metadata/access-limit");
        Assert.assertEquals(body(expect("PUT", "/v1/secrets/s1/metadata/access-limit")).toString(), "{\"key\":\"access-limit\",\"value\":\"12\"}");
        expect("DELETE", "/v1/secrets/s1/metadata/access-limit");
        expect("GET", "/v1/secrets/s1/consumers");
        RecordedRequest register = expect("POST", "/v1/secrets/s1/consumers");
        Assert.assertEquals(register.getHeader("OpenStack-API-Version"), "key-manager 1.1");
        Assert.assertEquals(body(register).toString(), "{\"service\":\"image\",\"resource_type\":\"image\",\"resource_id\":\"img1\"}");
        RecordedRequest remove = expect("DELETE", "/v1/secrets/s1/consumers");
        Assert.assertEquals(body(remove).toString(), "{\"service\":\"image\",\"resource_type\":\"image\",\"resource_id\":\"img1\"}");
        Assert.assertEquals(text, "beer");
        Assert.assertEquals(binary, new byte[] {1, 2});
        Assert.assertEquals(metadata.get("description"), "contains the AES key");
        Assert.assertEquals(item, "11");
        Assert.assertEquals(consumers.get(0).get("service"), "image");
    }

    public void containerSecretsAndConsumers() throws Exception {
        respondWith(201, "{\"container_ref\": \"http://127.0.0.1:9311/v1/containers/c1\"}");
        respondWith(204);
        respondWith(200, "{\"total\": 1, \"consumers\": [{\"name\": \"lb\", \"URL\": \"http://lb\", \"status\": \"ACTIVE\"}]}");
        respondWith(200, "{\"name\": \"container\", \"consumers\": [{\"name\": \"lb\", \"URL\": \"http://lb\"}]}");
        respondWith(200, "{\"name\": \"container\", \"consumers\": []}");

        var ext = osv3().barbican().containersExt();
        Assert.assertTrue(ext.addSecret("c1", "private_key", SECRET_REF).isSuccess());
        Assert.assertTrue(ext.removeSecret("c1", "private_key", SECRET_REF).isSuccess());
        List<Map<String, Object>> consumers = ext.listConsumers("c1");
        Assert.assertTrue(ext.registerConsumer("c1", "lb", "http://lb").isSuccess());
        Assert.assertTrue(ext.removeConsumer("c1", "lb", "http://lb").isSuccess());

        Assert.assertEquals(body(expect("POST", "/v1/containers/c1/secrets")).toString(), "{\"name\":\"private_key\",\"secret_ref\":\"" + SECRET_REF + "\"}");
        Assert.assertEquals(body(expect("DELETE", "/v1/containers/c1/secrets")).toString(), "{\"name\":\"private_key\",\"secret_ref\":\"" + SECRET_REF + "\"}");
        expect("GET", "/v1/containers/c1/consumers");
        Assert.assertEquals(body(expect("POST", "/v1/containers/c1/consumers")).toString(), "{\"name\":\"lb\",\"URL\":\"http://lb\"}");
        Assert.assertEquals(body(expect("DELETE", "/v1/containers/c1/consumers")).toString(), "{\"name\":\"lb\",\"URL\":\"http://lb\"}");
        Assert.assertEquals(consumers.get(0).get("URL"), "http://lb");
    }

    public void ordersQuotasStores() throws Exception {
        String order = "{\"created\": \"2015-10-20T18:49:02\", \"creator_id\": \"u1\", \"meta\": {\"algorithm\": \"AES\", \"bit_length\": 256, \"name\": \"secretname\"},"
                + " \"order_ref\": \"http://127.0.0.1:9311/v1/orders/o1\", \"secret_ref\": \"" + SECRET_REF + "\", \"status\": \"ACTIVE\", \"type\": \"key\"}";
        String store = "{\"status\": \"ACTIVE\", \"name\": \"PKCS11 HSM\", \"global_default\": true, \"secret_store_plugin\": \"store_crypto\","
                + " \"crypto_plugin\": \"p11_crypto\", \"secret_store_ref\": \"http://127.0.0.1:9311/v1/secret-stores/st1\"}";
        respondWith(202, "{\"order_ref\": \"http://127.0.0.1:9311/v1/orders/o1\"}");
        respondWith(200, "{\"orders\": [" + order + "], \"total\": 1}");
        respondWith(200, order);
        respondWith(204);
        respondWith(200, "{\"quotas\": {\"secrets\": 10, \"orders\": 20, \"containers\": 10, \"consumers\": -1, \"cas\": 5}}");
        respondWith(200, "{\"project_quotas\": [{\"project_id\": \"1234\", \"project_quotas\": {\"secrets\": 2000, \"orders\": 0}}], \"total\": 1}");
        respondWith(200, "{\"project_quotas\": {\"secrets\": 10, \"orders\": 20}}");
        respondWith(204);
        respondWith(204);
        respondWith(200, "{\"secret_stores\": [" + store + "]}");
        respondWith(200, store);
        respondWith(200, store);
        respondWith(200, store);
        respondWith(204);
        respondWith(204);

        var barbican = osv3().barbican();
        String ref = barbican.orders().create("key", Map.of("name", "secretname", "algorithm", "AES", "bit_length", 256));
        List<? extends Order> orders = barbican.orders().list(Map.of("limit", "10"));
        Order one = barbican.orders().get("o1");
        Assert.assertTrue(barbican.orders().delete("o1").isSuccess());
        Map<String, Integer> effective = barbican.quotas().effective();
        Map<String, Map<String, Integer>> all = barbican.quotas().listProjectQuotas();
        Map<String, Integer> project = barbican.quotas().getProjectQuotas("1234");
        Assert.assertTrue(barbican.quotas().setProjectQuotas("1234", Map.of("secrets", 50)).isSuccess());
        Assert.assertTrue(barbican.quotas().deleteProjectQuotas("1234").isSuccess());
        List<? extends SecretStore> stores = barbican.secretStores().list();
        barbican.secretStores().get("st1");
        SecretStore global = barbican.secretStores().globalDefault();
        barbican.secretStores().preferred();
        Assert.assertTrue(barbican.secretStores().setPreferred("st1").isSuccess());
        Assert.assertTrue(barbican.secretStores().unsetPreferred("st1").isSuccess());

        RecordedRequest create = expect("POST", "/v1/orders");
        Assert.assertEquals(body(create).get("type").asText(), "key");
        Assert.assertEquals(body(create).get("meta").get("bit_length").asInt(), 256);
        expect("GET", "/v1/orders?limit=10");
        expect("GET", "/v1/orders/o1");
        expect("DELETE", "/v1/orders/o1");
        expect("GET", "/v1/quotas");
        expect("GET", "/v1/project-quotas");
        expect("GET", "/v1/project-quotas/1234");
        Assert.assertEquals(body(expect("PUT", "/v1/project-quotas/1234")).toString(), "{\"project_quotas\":{\"secrets\":50}}");
        expect("DELETE", "/v1/project-quotas/1234");
        expect("GET", "/v1/secret-stores");
        expect("GET", "/v1/secret-stores/st1");
        expect("GET", "/v1/secret-stores/global-default");
        expect("GET", "/v1/secret-stores/preferred");
        expect("POST", "/v1/secret-stores/st1/preferred");
        expect("DELETE", "/v1/secret-stores/st1/preferred");
        Assert.assertEquals(ref, "http://127.0.0.1:9311/v1/orders/o1");
        Assert.assertEquals(orders.get(0).getStatus(), "ACTIVE");
        Assert.assertEquals(one.getSecretRef(), SECRET_REF);
        Assert.assertEquals(one.getMeta().get("bit_length"), 256);
        Assert.assertEquals(effective.get("consumers"), Integer.valueOf(-1));
        Assert.assertEquals(all.get("1234").get("secrets"), Integer.valueOf(2000));
        Assert.assertEquals(project.get("orders"), Integer.valueOf(20));
        Assert.assertEquals(stores.get(0).getName(), "PKCS11 HSM");
        Assert.assertEquals(global.isGlobalDefault(), Boolean.TRUE);
    }

    public void missingOrderIsNull() throws Exception {
        respondWith(404, "{\"code\": 404, \"title\": \"Not Found\", \"description\": \"Order not found.\"}");
        Assert.assertNull(osv3().barbican().orders().get("missing"));
        takeRequest();
    }
}
