package org.openstack4j.api.octavia.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.octavia.ext.L7Policy;
import org.openstack4j.model.octavia.ext.L7Rule;
import org.openstack4j.model.octavia.options.L7PolicyOptions;
import org.openstack4j.model.octavia.options.L7RuleOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Octavia/Ext/L7")
public class L7PolicyTests extends AbstractOctaviaExtTest {

    private static final String POLICY = "{\"listener_id\": \"023f2e34\", \"description\": \"Redirect\", \"admin_state_up\": true, \"rules\": [{\"id\": \"efd6a3f8\"}], \"provisioning_status\": \"ACTIVE\","
            + " \"redirect_http_code\": 302, \"redirect_pool_id\": null, \"redirect_prefix\": null, \"redirect_url\": \"http://www.example.com\", \"action\": \"REDIRECT_TO_URL\", \"position\": 1,"
            + " \"project_id\": \"p1\", \"id\": \"8a1412f0\", \"operating_status\": \"ONLINE\", \"name\": \"redirect-example.com\", \"tags\": [\"test_tag\"]}";
    private static final String RULE = "{\"compare_type\": \"REGEX\", \"provisioning_status\": \"ACTIVE\", \"invert\": false, \"admin_state_up\": true, \"value\": \"/images*\", \"key\": null,"
            + " \"project_id\": \"p1\", \"type\": \"PATH\", \"id\": \"16621dbb\", \"operating_status\": \"ONLINE\", \"tags\": [\"test_tag\"]}";

    public void l7Policies() throws Exception {
        respondWith(201, "{\"l7policy\": " + POLICY + "}");
        respondWith(200, "{\"l7policies\": [" + POLICY + "]}");
        respondWith(200, "{\"l7policies\": []}");
        respondWith(200, "{\"l7policy\": " + POLICY + "}");
        respondWith(200, "{\"l7policy\": " + POLICY + "}");
        respondWith(204);

        var l7 = osv3().octavia().l7Policies();
        L7Policy created = l7.create(L7PolicyOptions.create("023f2e34", "REDIRECT_TO_URL").redirectUrl("http://www.example.com").redirectHttpCode(302).position(1));
        List<? extends L7Policy> all = l7.list();
        l7.list(Map.of("listener_id", "023f2e34"));
        l7.get("8a1412f0");
        l7.update("8a1412f0", L7PolicyOptions.update().name("renamed"));
        Assert.assertTrue(l7.delete("8a1412f0").isSuccess());

        RecordedRequest create = expect("POST", "/v2.0/lbaas/l7policies");
        var body = body(create).get("l7policy");
        Assert.assertEquals(body.get("listener_id").asText(), "023f2e34");
        Assert.assertEquals(body.get("redirect_http_code").asInt(), 302);
        Assert.assertFalse(body.has("redirect_pool_id"));
        expect("GET", "/v2.0/lbaas/l7policies");
        Assert.assertTrue(decodedPath(takeRequest()).endsWith("/v2.0/lbaas/l7policies?listener_id=023f2e34"));
        expect("GET", "/v2.0/lbaas/l7policies/8a1412f0");
        Assert.assertEquals(body(expect("PUT", "/v2.0/lbaas/l7policies/8a1412f0")).get("l7policy").size(), 1);
        expect("DELETE", "/v2.0/lbaas/l7policies/8a1412f0");
        Assert.assertEquals(created.getAction(), "REDIRECT_TO_URL");
        Assert.assertNull(created.getRedirectPoolId());
        Assert.assertEquals(all.get(0).getRules().get(0).get("id"), "efd6a3f8");
    }

    public void l7Rules() throws Exception {
        String r = "/v2.0/lbaas/l7policies/8a1412f0/rules";
        respondWith(201, "{\"rule\": " + RULE + "}");
        respondWith(200, "{\"rules\": [" + RULE + "]}");
        respondWith(200, "{\"rule\": " + RULE + "}");
        respondWith(200, "{\"rule\": " + RULE + "}");
        respondWith(204);

        var l7 = osv3().octavia().l7Policies();
        L7Rule created = l7.createRule("8a1412f0", L7RuleOptions.create("PATH", "REGEX", "/images*"));
        List<? extends L7Rule> rules = l7.listRules("8a1412f0");
        l7.getRule("8a1412f0", "16621dbb");
        l7.updateRule("8a1412f0", "16621dbb", L7RuleOptions.update().invert(true));
        l7.deleteRule("8a1412f0", "16621dbb");

        RecordedRequest create = expect("POST", r);
        Assert.assertEquals(body(create).get("rule").get("compare_type").asText(), "REGEX");
        Assert.assertFalse(body(create).get("rule").has("key"));
        expect("GET", r);
        expect("GET", r + "/16621dbb");
        Assert.assertTrue(body(expect("PUT", r + "/16621dbb")).get("rule").get("invert").asBoolean());
        expect("DELETE", r + "/16621dbb");
        Assert.assertEquals(created.getType(), "PATH");
        Assert.assertNull(created.getKey());
        Assert.assertFalse(rules.get(0).isInvert());
    }
}
