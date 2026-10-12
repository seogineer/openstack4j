package org.openstack4j.api.octavia.ext;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.octavia.ext.AvailabilityZoneProfile;
import org.openstack4j.model.octavia.ext.FlavorProfile;
import org.openstack4j.model.octavia.ext.OctaviaFlavor;
import org.openstack4j.model.octavia.options.AvailabilityZoneProfileOptions;
import org.openstack4j.model.octavia.options.FlavorProfileOptions;
import org.openstack4j.model.octavia.options.OctaviaAvailabilityZoneOptions;
import org.openstack4j.model.octavia.options.OctaviaFlavorOptions;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Runs against a real Octavia when OS_AUTH_URL is set; every temporary resource is removed in finally. */
@Test(suiteName = "Octavia/Ext/Live", groups = "octavia-live", singleThreaded = true)
public class OctaviaExtensionsLiveTests {

    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live octavia tests");
        String authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        String token = System.getenv("OS_TOKEN");
        Identifier project = Identifier.byName(env("OS_PROJECT_NAME", null));
        Identifier projectDomain = Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"));
        os = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToProject(project, projectDomain).authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null),
                Identifier.byName(env("OS_USER_DOMAIN_NAME", "Default"))).scopeToProject(project, projectDomain).authenticate();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live octavia tests");
        }
        return value;
    }

    /** Unique per run so a run that died before its cleanup cannot block the next one (AZ names are primary keys). */
    private static final String SUFFIX = Long.toString(System.currentTimeMillis(), 36);

    /** Admin-only Octavia calls answer 403 to a member token; that is the environment, not a failure. */
    private static <T> T adminOnly(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (ClientResponseException e) {
            if (e.getStatus() == 403)
                throw new SkipException("needs an admin token: " + e.getMessage());
            throw e;
        }
    }

    /** Flavor and AZ profiles name a provider driver; skip when this cloud does not run it. */
    private void requireProvider(String provider) {
        if (adminOnly(() -> os.octavia().providers().list()).stream().noneMatch(p -> provider.equals(p.getName())))
            throw new SkipException("Octavia provider '" + provider + "' not enabled");
    }

    public void providersAndCapabilities() {
        requireProvider("amphora");
        Assert.assertTrue(adminOnly(() -> os.octavia().providers().flavorCapabilities("amphora")).stream().anyMatch(c -> "loadbalancer_topology".equals(c.getName())));
    }

    public void quotas() {
        Assert.assertNotNull(adminOnly(() -> os.octavia().quotas().defaults()).getLoadbalancer());
        Assert.assertNotNull(adminOnly(() -> os.octavia().quotas().get(os.getToken().getProject().getId())).getLoadbalancer());
    }

    public void flavorProfileAndFlavor() {
        requireProvider("amphora");
        FlavorProfile profile = adminOnly(() -> os.octavia().flavorProfiles().create(FlavorProfileOptions.create("os4j-live-fp-" + SUFFIX, "amphora", "{\"loadbalancer_topology\": \"SINGLE\"}")));
        boolean profileDeleted = false;
        try {
            OctaviaFlavor flavor = adminOnly(() -> os.octavia().flavors().create(OctaviaFlavorOptions.create("os4j-live-flavor-" + SUFFIX, profile.getId())));
            boolean flavorDeleted;
            try {
                Assert.assertEquals(os.octavia().flavors().get(flavor.getId()).getFlavorProfileId(), profile.getId());
            } finally {
                flavorDeleted = os.octavia().flavors().delete(flavor.getId()).isSuccess();
            }
            Assert.assertTrue(flavorDeleted, "flavor not deleted");
        } finally {
            profileDeleted = os.octavia().flavorProfiles().delete(profile.getId()).isSuccess();
        }
        Assert.assertTrue(profileDeleted, "flavor profile not deleted");
    }

    public void availabilityZoneProfileAndZone() {
        requireProvider("amphora");
        AvailabilityZoneProfile profile = adminOnly(() -> os.octavia().availabilityZoneProfiles().create(AvailabilityZoneProfileOptions.create("os4j-live-azp-" + SUFFIX, "amphora", "{\"compute_zone\": \"" + System.getenv().getOrDefault("OS_COMPUTE_ZONE", "nova") + "\"}")));
        boolean profileDeleted;
        try {
            String zone = "os4j-live-az-" + SUFFIX;
            adminOnly(() -> os.octavia().availabilityZones().create(OctaviaAvailabilityZoneOptions.create(zone, profile.getId())));
            boolean zoneDeleted;
            try {
                Assert.assertEquals(os.octavia().availabilityZones().get(zone).getAvailabilityZoneProfileId(), profile.getId());
            } finally {
                zoneDeleted = os.octavia().availabilityZones().delete(zone).isSuccess();
            }
            Assert.assertTrue(zoneDeleted, "availability zone not deleted");
        } finally {
            profileDeleted = os.octavia().availabilityZoneProfiles().delete(profile.getId()).isSuccess();
        }
        Assert.assertTrue(profileDeleted, "availability zone profile not deleted");
    }

    public void amphoraeList() {
        Assert.assertNotNull(adminOnly(() -> os.octavia().amphorae().list()));
    }
}
