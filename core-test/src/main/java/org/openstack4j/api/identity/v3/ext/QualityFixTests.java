package org.openstack4j.api.identity.v3.ext;

import java.util.Map;

import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.v3.options.ApplicationCredentialCreate;
import org.openstack4j.model.identity.v3.options.LimitCreate;
import org.openstack4j.model.identity.v3.options.RegisteredLimitCreate;
import org.openstack4j.model.identity.v3.options.TrustCreate;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Deferred minors from the identity review: misconfigured builders fail clearly, option builders omit nulls. */
@Test(suiteName = "Identity/V3/QualityFixes")
public class QualityFixTests extends AbstractIdentityExtTest {

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*secret.*")
    public void applicationCredentialWithoutSecretFailsClearly() throws Exception {
        try {
            OSFactory.builderV3().endpoint(authURL("/v3")).applicationCredential("a05ec0a2", null).authenticate();
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*passcode.*")
    public void passcodeWithoutUserFailsClearly() throws Exception {
        try {
            OSFactory.builderV3().endpoint(authURL("/v3")).passcode("123456").authenticate();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void optionBuildersOmitNulls() {
        Assert.assertEquals(ApplicationCredentialCreate.create("ac").description(null).secret(null).toMap(), Map.of("name", "ac"));
        Assert.assertFalse(TrustCreate.create("u1", "u2", false).projectId(null).toMap().containsKey("project_id"));
        Assert.assertFalse(LimitCreate.forProject("p1", "s1", "cores", 10).regionId(null).description(null).toMap().containsKey("region_id"));
        Assert.assertFalse(RegisteredLimitCreate.create("s1", "cores", 10).regionId(null).toMap().containsKey("region_id"));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void applicationCredentialByNameNeedsUser() {
        OSFactory.builderV3().endpoint(authURL("/v3")).applicationCredential("ac", "s", (Identifier) null, null).authenticate();
    }
}
