package org.openstack4j.api.identity.v2;

import static org.testng.Assert.assertEquals;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.identity.URLResolverParams;
import org.openstack4j.openstack.identity.internal.DefaultEndpointURLResolver;
import org.openstack4j.openstack.identity.v2.domain.Credentials;
import org.openstack4j.openstack.identity.v2.domain.KeystoneAccess;
import org.testng.annotations.Test;

/**
 * The Keystone v2 type fallback must not answer with an entry that is registered under the same type
 * for another service (Zun's "container" next to Magnum's).
 */
public class KeystoneV2CatalogFallbackTest {

    private static final String AUTH = "http://127.0.0.1:5000/v2.0";
    private static final String ZUN = "http://127.0.0.1:9517/v1";
    private static final String MAGNUM = "http://127.0.0.1:9511/v1";

    private static String entry(String name, String url) {
        return "{\"type\":\"container\",\"name\":\"" + name + "\",\"endpoints\":[{\"region\":\"RegionOne\","
                + "\"publicURL\":\"" + url + "\",\"internalURL\":\"" + url + "\",\"adminURL\":\"" + url + "\"}]}";
    }

    private static KeystoneAccess access(String tenantId, String... entries) throws Exception {
        String json = "{\"access\":{\"token\":{\"id\":\"t\",\"expires\":\"2099-01-01T00:00:00Z\","
                + "\"tenant\":{\"id\":\"" + tenantId + "\",\"name\":\"p\"}},\"serviceCatalog\":["
                + String.join(",", entries) + "]}}";
        return ObjectMapperSingleton.getContext(KeystoneAccess.class).readValue(json, KeystoneAccess.class)
                .applyContext(AUTH, new Credentials("test", "test"));
    }

    private static String find(KeystoneAccess access, ServiceType type) {
        return new DefaultEndpointURLResolver().findURLV2(URLResolverParams.create(access, type));
    }

    @Test
    public void onlyZunInCatalogMagnumDoesNotUseIt() throws Exception {
        KeystoneAccess access = access("v2-fallback-a", entry("zun", ZUN));
        assertEquals(find(access, ServiceType.MAGNUM), AUTH);
        assertEquals(find(access, ServiceType.CONTAINER_APP), ZUN);
    }

    @Test
    public void magnumAndZunEachGetTheirOwn() throws Exception {
        KeystoneAccess access = access("v2-fallback-b", entry("magnum", MAGNUM), entry("zun", ZUN));
        assertEquals(find(access, ServiceType.MAGNUM), MAGNUM);
        assertEquals(find(access, ServiceType.CONTAINER_APP), ZUN);
    }

    @Test
    public void unknownNameStillFallsBackToType() throws Exception {
        KeystoneAccess access = access("v2-fallback-c", entry("my-magnum", MAGNUM));
        assertEquals(find(access, ServiceType.MAGNUM), MAGNUM);
    }
}
