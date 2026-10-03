package org.openstack4j.api.identity.v3.ext;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.Trust;
import org.openstack4j.model.identity.v3.options.TrustCreate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Identity/V3/Trusts")
public class TrustTests extends AbstractIdentityExtTest {

    private static final String TRUST = "c947c7f50eff44afaaa94782b90fd395";
    private static final String ROLE = "1222de53b11f40e68497383c7636fbe0";
    private static final String ROLE_JSON = "{\"id\": \"" + ROLE + "\", \"name\": \"admin\", \"domain_id\": null, \"description\": null, \"options\": {\"immutable\": true}, \"links\": {}}";
    private static final String TRUST_JSON = "{\"roles\": [" + ROLE_JSON + "], \"id\": \"" + TRUST + "\", \"trustor_user_id\": \"" + USER + "\","
            + " \"trustee_user_id\": \"" + USER + "\", \"project_id\": \"" + PROJECT + "\", \"impersonation\": false, \"expires_at\": null,"
            + " \"remaining_uses\": null, \"deleted_at\": null, \"redelegated_trust_id\": null, \"redelegation_count\": 0,"
            + " \"roles_links\": {\"self\": \"x\", \"next\": null, \"previous\": null}, \"links\": {}}";

    public void trustLifecycle() throws Exception {
        respondWith(201, "{\"trust\": " + TRUST_JSON + "}");
        respondWith(200, "{\"trusts\": [" + TRUST_JSON + "], \"links\": {}}");
        respondWith(200, "{\"trusts\": [], \"links\": {}}");
        respondWith(200, "{\"trust\": " + TRUST_JSON + "}");
        respondWith(200, "{\"roles\": [" + ROLE_JSON + "], \"links\": {}}");
        respondWith(200, "{\"role\": " + ROLE_JSON + "}");
        respondWith(200);
        respondWith(204);

        Trust created = osv3().identity().trusts().create(TrustCreate.create(USER, USER, false).projectId(PROJECT).roleNames("admin").remainingUses(3));
        List<? extends Trust> all = osv3().identity().trusts().list();
        osv3().identity().trusts().list(USER, null);
        Trust one = osv3().identity().trusts().get(TRUST);
        List<? extends Role> roles = osv3().identity().trusts().roles(TRUST);
        Role role = osv3().identity().trusts().getRole(TRUST, ROLE);
        boolean has = osv3().identity().trusts().checkRole(TRUST, ROLE).isSuccess();
        boolean deleted = osv3().identity().trusts().delete(TRUST).isSuccess();

        RecordedRequest create = takeRequest();
        Assert.assertTrue(create.getPath().endsWith("/v3/OS-TRUST/trusts"));
        JsonNode body = body(create).get("trust");
        Assert.assertEquals(body.get("trustor_user_id").asText(), USER);
        Assert.assertFalse(body.get("impersonation").asBoolean());
        Assert.assertEquals(body.get("project_id").asText(), PROJECT);
        Assert.assertEquals(body.get("roles").get(0).get("name").asText(), "admin");
        Assert.assertEquals(body.get("remaining_uses").asInt(), 3);
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-TRUST/trusts"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-TRUST/trusts?trustor_user_id=" + USER));
        Assert.assertTrue(takeRequest().getPath().endsWith("/v3/OS-TRUST/trusts/" + TRUST));
        Assert.assertTrue(takeRequest().getPath().endsWith("/trusts/" + TRUST + "/roles"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/trusts/" + TRUST + "/roles/" + ROLE));
        Assert.assertEquals(takeRequest().getMethod(), "HEAD");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getTrustorUserId(), USER);
        Assert.assertEquals(created.getRedelegationCount(), Integer.valueOf(0));
        Assert.assertEquals(all.get(0).getRoles().get(0).getName(), "admin");
        Assert.assertFalse(one.getImpersonation());
        Assert.assertNull(one.getRemainingUses());
        Assert.assertEquals(roles.size(), 1);
        Assert.assertEquals(role.getId(), ROLE);
        Assert.assertTrue(has);
        Assert.assertTrue(deleted);
    }
}
