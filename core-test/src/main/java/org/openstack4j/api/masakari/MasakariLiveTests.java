package org.openstack4j.api.masakari;

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.ext.Service;
import org.openstack4j.model.masakari.Host;
import org.openstack4j.model.masakari.Segment;
import org.openstack4j.model.masakari.options.HostOptions;
import org.openstack4j.model.masakari.options.SegmentOptions;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Runs against a real Masakari when OS_AUTH_URL is set; every temporary resource is removed in finally. */
@Test(suiteName = "Masakari/Live", groups = "masakari-live", singleThreaded = true)
public class MasakariLiveTests {

    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live masakari tests");
        String authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        String token = System.getenv("OS_TOKEN");
        Identifier project = Identifier.byName(env("OS_PROJECT_NAME", null));
        Identifier projectDomain = Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"));
        os = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToProject(project, projectDomain).authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null),
                Identifier.byName(env("OS_USER_DOMAIN_NAME", "Default"))).scopeToProject(project, projectDomain).authenticate();
        requireService(ServiceType.INSTANCE_HA);
        cleanupLeftovers();
    }

    private void requireService(ServiceType type) {
        if (os.getToken().getCatalog().stream().noneMatch(s -> ServiceType.forCatalogEntry(s.getType(), s.getName()) == type))
            throw new SkipException(type + " not in the service catalog");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live masakari tests");
        }
        return value;
    }

    /** Unique per run so a run that died before its cleanup cannot collide with the next one. */
    private static final String SUFFIX = Long.toString(System.currentTimeMillis(), 36);

    /** Masakari's default policy is admin-only; a member token gets 403, which is the environment, not a failure. */
    private static <T> T adminOnly(Supplier<T> call) {
        try {
            return call.get();
        } catch (ResponseException e) {
            if (e.getStatus() == 403)
                throw new SkipException("needs an admin token: " + e.getMessage());
            throw e;
        }
    }

    /** Skips on a status that only says this cloud is not configured for the feature. */
    private static <T> T skipOn(int status, String why, Supplier<T> call) {
        try {
            return call.get();
        } catch (ResponseException e) {
            if (e.getStatus() == status)
                throw new SkipException(why + ": " + e.getMessage());
            throw e;
        }
    }

    /** Best effort: a run that died earlier left segments (and hosts, which block segment deletion) behind. */
    private void cleanupLeftovers() {
        try {
            for (Segment s : os.masakari().segments().list())
                if (s.getName() != null && s.getName().startsWith("os4j-live-"))
                    try {
                        for (Host h : os.masakari().hosts().list(s.getUuid(), null))
                            os.masakari().hosts().delete(s.getUuid(), h.getUuid());
                        os.masakari().segments().delete(s.getUuid());
                    } catch (RuntimeException ignored) {
                    }
        } catch (RuntimeException ignored) {
        }
    }

    /** Masakari asks nova about the host it registers, so the name must be a real nova-compute host. */
    private String computeHost() {
        String host = System.getenv("OS_COMPUTE_HOST");
        if (host != null && !host.isEmpty()) return host;
        return adminOnly(() -> os.compute().services().list()).stream()
                .filter(s -> "nova-compute".equals(s.getBinary()))
                .map(Service::getHost).findFirst()
                .orElseThrow(() -> new SkipException("no nova-compute host; set OS_COMPUTE_HOST"));
    }

    public void listSegmentsAndNotifications() {
        Assert.assertNotNull(adminOnly(() -> os.masakari().segments().list()));
        Assert.assertTrue(os.masakari().segments().list(Map.of("recovery_method", "auto")).stream()
                .allMatch(s -> "auto".equals(s.getRecoveryMethod())));
        Assert.assertTrue(os.masakari().notifications().list(Map.of("limit", "5")).size() <= 5);
    }

    public void segmentLifecycle() {
        Segment s = adminOnly(() -> os.masakari().segments().create(
                SegmentOptions.create("os4j-live-seg-" + SUFFIX, "COMPUTE", "auto").description("os4j live")));
        boolean deleted;
        try {
            Segment got = os.masakari().segments().get(s.getUuid());
            Assert.assertEquals(got.getName(), "os4j-live-seg-" + SUFFIX);
            Assert.assertEquals(got.getServiceType(), "COMPUTE");
            Assert.assertEquals(got.getRecoveryMethod(), "auto");
            Assert.assertEquals(os.masakari().segments().update(s.getUuid(), SegmentOptions.update().description("os4j live updated")).getDescription(), "os4j live updated");
            Assert.assertEquals(os.masakari().segments().get(s.getUuid()).getDescription(), "os4j live updated");
        } finally {
            deleted = os.masakari().segments().delete(s.getUuid()).isSuccess();
        }
        Assert.assertTrue(deleted, "segment not deleted");
        Assert.assertNull(os.masakari().segments().get(s.getUuid()));
    }

    /** The enabled flag needs microversion 1.2; kept apart so a 406 skip cannot hide the lifecycle result. */
    public void segmentEnabledToggle() {
        Segment s = adminOnly(() -> os.masakari().segments().create(SegmentOptions.create("os4j-live-seg2-" + SUFFIX, "COMPUTE", "auto")));
        boolean deleted;
        try {
            Segment updated = skipOn(406, "instance-ha 1.2 not supported",
                    () -> os.masakari().segments().update(s.getUuid(), SegmentOptions.update().enabled(false)));
            Assert.assertEquals(updated.isEnabled(), Boolean.FALSE);
        } finally {
            deleted = os.masakari().segments().delete(s.getUuid()).isSuccess();
        }
        Assert.assertTrue(deleted, "segment not deleted");
    }

    public void hostLifecycle() {
        String hostName = computeHost();
        Segment s = adminOnly(() -> os.masakari().segments().create(SegmentOptions.create("os4j-live-hostseg-" + SUFFIX, "COMPUTE", "auto")));
        String segUuid = s.getUuid();
        boolean segmentDeleted;
        try {
            Host h = skipOn(409, "host already in another segment",
                    () -> os.masakari().hosts().create(segUuid, HostOptions.create(hostName, "COMPUTE", "SSH")));
            String hUuid = h.getUuid();
            boolean hostDeleted;
            try {
                Host got = os.masakari().hosts().get(segUuid, hUuid);
                Assert.assertEquals(got.getName(), hostName);
                Assert.assertEquals(got.getFailoverSegmentId(), segUuid);
                Assert.assertEquals(got.isReserved(), Boolean.FALSE);
                Assert.assertEquals(os.masakari().hosts().update(segUuid, hUuid, HostOptions.update().reserved(true)).isReserved(), Boolean.TRUE);
                Assert.assertTrue(os.masakari().hosts().list(segUuid, Map.of("reserved", "1")).stream().anyMatch(x -> hUuid.equals(x.getUuid())));
                Assert.assertEquals(os.masakari().hosts().update(segUuid, hUuid, HostOptions.update().reserved(false)).isReserved(), Boolean.FALSE);
            } finally {
                hostDeleted = os.masakari().hosts().delete(segUuid, hUuid).isSuccess();
            }
            Assert.assertTrue(hostDeleted, "host not deleted");
            Assert.assertNull(os.masakari().hosts().get(segUuid, hUuid));
        } finally {
            // after the host: Masakari answers 409 for a segment that still has hosts
            segmentDeleted = os.masakari().segments().delete(segUuid).isSuccess();
        }
        Assert.assertTrue(segmentDeleted, "segment not deleted");
    }

    public void missingSegmentIsNull() {
        Assert.assertNull(adminOnly(() -> os.masakari().segments().get(UUID.randomUUID().toString())));
    }
}
