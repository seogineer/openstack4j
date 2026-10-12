package org.openstack4j.api.dns.v2;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.dns.v2.Recordset;
import org.openstack4j.model.dns.v2.Status;
import org.openstack4j.model.dns.v2.Zone;
import org.openstack4j.model.dns.v2.ext.Blacklist;
import org.openstack4j.model.dns.v2.ext.Tld;
import org.openstack4j.model.dns.v2.ext.TsigKey;
import org.openstack4j.model.dns.v2.ext.ZoneExport;
import org.openstack4j.model.dns.v2.ext.ZoneTransferRequest;
import org.openstack4j.model.dns.v2.options.BlacklistCreate;
import org.openstack4j.model.dns.v2.options.BlacklistUpdate;
import org.openstack4j.model.dns.v2.options.TsigKeyCreate;
import org.openstack4j.model.dns.v2.options.TsigKeyUpdate;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Runs against a real Designate when OS_AUTH_URL is set; one zone per class, removed in {@link #deleteZone()}. */
@Test(suiteName = "Designate/Live", groups = "dns-live", singleThreaded = true)
public class DesignateLiveTests {

    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live designate tests");
        String authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        String token = System.getenv("OS_TOKEN");
        Identifier project = Identifier.byName(env("OS_PROJECT_NAME", null));
        Identifier projectDomain = Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"));
        os = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToProject(project, projectDomain).authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null),
                Identifier.byName(env("OS_USER_DOMAIN_NAME", "Default"))).scopeToProject(project, projectDomain).authenticate();
        requireService(ServiceType.DNS);
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
            throw new SkipException(name + " not set; skipping live designate tests");
        }
        return value;
    }

    /** Unique per run so a run that died before its cleanup cannot collide with the next one. */
    private static final String SUFFIX = Long.toString(System.currentTimeMillis(), 36);

    /** Designate wants an absolute name: no leading dot, one trailing dot. */
    private static String normalize(String domain) {
        String d = domain.trim().replaceAll("^\\.+", "");
        return d.endsWith(".") ? d : d + ".";
    }

    private static final String DOMAIN = normalize(System.getenv().getOrDefault("OS_DNS_ZONE_SUFFIX", "example.org."));

    private final String zoneName = "os4j-live-" + SUFFIX + "." + DOMAIN;

    private String zoneId;

    /** Admin-only calls answer 403 to a member token; that is the environment, not a failure. */
    private static <T> T adminOnly(Supplier<T> call) {
        try {
            return call.get();
        } catch (ResponseException e) {
            if (e.getStatus() == 403)
                throw new SkipException("needs an admin token: " + e.getMessage());
            throw e;
        }
    }

    /** Skips on a status that only says this cloud is not configured for the feature (ResponseException also covers 5xx). */
    private static <T> T skipOn(int status, String why, Supplier<T> call) {
        try {
            return call.get();
        } catch (ResponseException e) {
            if (e.getStatus() == status)
                throw new SkipException(why + ": " + e.getMessage());
            throw e;
        }
    }

    private static <T> T waitUntil(String what, int seconds, Supplier<T> fetch, Predicate<T> done, Predicate<T> failed)
            throws InterruptedException {
        T last = null;
        for (int i = 0; i < seconds; i++) {
            last = fetch.get();
            if (last != null && done.test(last)) return last;
            if (last != null && failed.test(last)) Assert.fail(what + " failed: " + last);
            Thread.sleep(1000);
        }
        Assert.fail(what + " not reached in " + seconds + "s: " + last);
        return last;
    }

    /** Like waitUntil but a timeout is an answer, not a failure; fetch may return null (deleted). */
    private static <T> boolean poll(int seconds, Supplier<T> fetch, Predicate<T> done) throws InterruptedException {
        for (int i = 0; i < seconds; i++) {
            if (done.test(fetch.get())) return true;
            Thread.sleep(1000);
        }
        return false;
    }

    /** One zone per class, created on first use. A zone that never reaches ACTIVE (slow bind9) is still usable for the API round trips. */
    private String zone() throws InterruptedException {
        if (zoneId == null) {
            String email = "hostmaster@" + DOMAIN.replaceAll("\\.$", "");
            Zone created;
            try {
                created = os.dns().zones().create(zoneName, email);
            } catch (ResponseException e) {
                if (e.getStatus() == 400 && !tldRegistered())
                    throw new SkipException("TLD of " + DOMAIN + " not registered; set OS_DNS_ZONE_SUFFIX: " + e.getMessage());
                throw e;
            }
            zoneId = created.getId();
            poll(30, () -> os.dns().zones().get(zoneId), z -> z != null && z.getStatus() == Status.ACTIVE);
        }
        return zoneId;
    }

    /** With TLD validation on, Designate refuses zones under a TLD nobody registered; without any TLD it accepts all. */
    private boolean tldRegistered() {
        String name = DOMAIN.replaceAll("\\.$", "");
        String tld = name.substring(name.lastIndexOf('.') + 1);
        try {
            List<? extends Tld> tlds = os.dns().tlds().list();
            return tlds.isEmpty() || tlds.stream().anyMatch(t -> tld.equalsIgnoreCase(t.getName()));
        } catch (ResponseException e) {
            return true;
        }
    }

    /** Best effort: a run that died earlier left zones behind; nothing here may fail the class. */
    private void cleanupLeftovers() {
        try {
            for (Zone z : os.dns().zones().list())
                if (z.getName() != null && z.getName().startsWith("os4j-live-"))
                    try {
                        os.dns().zones().delete(z.getId());
                    } catch (RuntimeException ignored) {
                    }
        } catch (RuntimeException ignored) {
        }
        try {
            for (Blacklist b : os.dns().blacklists().list())
                if (b.getPattern() != null && b.getPattern().contains("blk-os4j-"))
                    try {
                        os.dns().blacklists().delete(b.getId());
                    } catch (RuntimeException ignored) {
                    }
            for (TsigKey k : os.dns().tsigKeys().list())
                if (k.getName() != null && k.getName().startsWith("os4j-live-tsig-"))
                    try {
                        os.dns().tsigKeys().delete(k.getId());
                    } catch (RuntimeException ignored) {
                    }
        } catch (RuntimeException ignored) {
        }
    }

    @AfterClass(alwaysRun = true)
    public void deleteZone() throws InterruptedException {
        if (zoneId == null) return;
        ActionResponse deleted = os.dns().zones().delete(zoneId);
        if (poll(60, () -> os.dns().zones().get(zoneId), z -> z == null)) return;
        // delete is asynchronous and bind9 may be slow; abandon drops the zone from Designate without waiting for the backend
        ActionResponse abandoned = os.dns().zones().abandon(zoneId);
        if (poll(10, () -> os.dns().zones().get(zoneId), z -> z == null)) return;
        Assert.fail("zone " + zoneName + " left behind; delete: " + deleted + ", abandon: " + abandoned);
    }

    public void readOnly() {
        Assert.assertFalse(os.dns().info().limits().isEmpty());
        Assert.assertFalse(adminOnly(() -> os.dns().pools().list()).isEmpty());
        Assert.assertFalse(adminOnly(() -> os.dns().serviceStatuses().list()).isEmpty());
        Assert.assertTrue(adminOnly(() -> os.dns().quotas().get(os.getToken().getProject().getId())).containsKey("zones"));
    }

    public void adminLists() {
        Assert.assertNotNull(adminOnly(() -> os.dns().tlds().list()));
        Assert.assertNotNull(os.dns().blacklists().list());
        Assert.assertNotNull(os.dns().tsigKeys().list());
    }

    /** Without Neutron integration Designate answers 500; kept apart so that skip does not hide the other asserts. */
    public void reverseFloatingIps() {
        Assert.assertNotNull(skipOn(500, "designate not wired to neutron", () -> os.dns().reverseFloatingIps().list()));
    }

    public void zoneAndRecordsetLifecycle() throws InterruptedException {
        String id = zone();
        Zone z = os.dns().zones().get(id);
        Assert.assertEquals(z.getName(), zoneName);
        Assert.assertTrue(z.getStatus() == Status.PENDING || z.getStatus() == Status.ACTIVE, "zone status " + z.getStatus());
        Assert.assertTrue(os.dns().zones().list().stream().anyMatch(x -> id.equals(x.getId())));
        Assert.assertFalse(os.dns().zones().listNameservers(id).isEmpty());

        String description = "os4j live " + SUFFIX;
        Assert.assertEquals(os.dns().zones().update(Builders.zone().id(id).description(description).build()).getDescription(), description);

        Recordset created = os.dns().recordsets().create(id, "www." + zoneName, "A", List.of("192.0.2.10"));
        String rsId = created.getId();
        boolean deleted;
        try {
            Recordset rs = os.dns().recordsets().get(id, rsId);
            Assert.assertEquals(rs.getType(), "A");
            Assert.assertEquals(rs.getRecords(), List.of("192.0.2.10"));
            Recordset updated = os.dns().recordsets().update(id, Builders.recordset().id(rsId).records(List.of("192.0.2.11")).build());
            Assert.assertEquals(updated.getRecords(), List.of("192.0.2.11"));
            List<? extends Recordset> found = os.dns().recordsets().list(id, Map.of("type", "A"));
            Assert.assertTrue(found.stream().anyMatch(r -> rsId.equals(r.getId())));
            Assert.assertTrue(found.stream().allMatch(r -> "A".equals(r.getType())));
        } finally {
            deleted = os.dns().recordsets().delete(id, rsId).isSuccess();
        }
        Assert.assertTrue(deleted, "recordset not deleted");
    }

    public void zoneExport() throws InterruptedException {
        ZoneExport export = os.dns().zoneFiles().export(zone());
        String exId = export.getId();
        boolean deleted;
        try {
            waitUntil("export COMPLETE", 30, () -> os.dns().zoneFiles().getExport(exId),
                    e -> "COMPLETE".equals(e.getStatus()), e -> "ERROR".equals(e.getStatus()));
            Assert.assertTrue(os.dns().zoneFiles().getExportContent(exId).contains(zoneName));
            Assert.assertTrue(os.dns().zoneFiles().listExports().stream().anyMatch(e -> exId.equals(e.getId())));
        } finally {
            deleted = os.dns().zoneFiles().deleteExport(exId).isSuccess();
        }
        Assert.assertTrue(deleted, "zone export not deleted");
    }

    /** The request is never accepted; it is removed before the class tears the zone down. */
    public void zoneTransferRequest() throws InterruptedException {
        String id = zone();
        ZoneTransferRequest request = os.dns().zoneTransfers().createRequest(id, null, "os4j live " + SUFFIX);
        boolean deleted;
        try {
            Assert.assertNotNull(request.getKey());
            Assert.assertEquals(request.getZoneId(), id);
            Assert.assertEquals(os.dns().zoneTransfers().getRequest(request.getId()).getDescription(), "os4j live " + SUFFIX);
            Assert.assertEquals(os.dns().zoneTransfers().updateRequest(request.getId(), null, "os4j live updated").getDescription(), "os4j live updated");
            Assert.assertTrue(os.dns().zoneTransfers().listRequests().stream().anyMatch(r -> request.getId().equals(r.getId())));
        } finally {
            deleted = os.dns().zoneTransfers().deleteRequest(request.getId()).isSuccess();
        }
        Assert.assertTrue(deleted, "zone transfer request not deleted");
    }

    public void blacklistLifecycle() {
        String pattern = "^blk-os4j-" + SUFFIX + "\\.invalid\\.$";
        Blacklist blacklist = adminOnly(() -> os.dns().blacklists().create(BlacklistCreate.create(pattern).description("os4j live")));
        boolean deleted;
        try {
            Assert.assertEquals(os.dns().blacklists().get(blacklist.getId()).getPattern(), pattern);
            Assert.assertEquals(os.dns().blacklists().update(blacklist.getId(), BlacklistUpdate.create().description("os4j live updated")).getDescription(), "os4j live updated");
            Assert.assertEquals(os.dns().blacklists().get(blacklist.getId()).getDescription(), "os4j live updated");
        } finally {
            deleted = os.dns().blacklists().delete(blacklist.getId()).isSuccess();
        }
        Assert.assertTrue(deleted, "blacklist not deleted");
        Assert.assertNull(os.dns().blacklists().get(blacklist.getId()));
    }

    public void tsigKeyLifecycle() {
        String poolId = adminOnly(() -> os.dns().pools().list()).get(0).getId();
        byte[] secret = new byte[16];
        new SecureRandom().nextBytes(secret);
        String name = "os4j-live-tsig-" + SUFFIX;
        TsigKey key = adminOnly(() -> os.dns().tsigKeys().create(TsigKeyCreate.create(name, "hmac-sha256", Base64.getEncoder().encodeToString(secret), "POOL", poolId)));
        boolean deleted;
        try {
            Assert.assertEquals(os.dns().tsigKeys().get(key.getId()).getName(), name);
            Assert.assertEquals(os.dns().tsigKeys().get(key.getId()).getAlgorithm(), "hmac-sha256");
            Assert.assertEquals(os.dns().tsigKeys().update(key.getId(), TsigKeyUpdate.create().name(name + "-u")).getName(), name + "-u");
        } finally {
            deleted = os.dns().tsigKeys().delete(key.getId()).isSuccess();
        }
        Assert.assertTrue(deleted, "tsig key not deleted");
    }

    /** The project may have had its own override, so after reset the original number is put back when it differs. */
    public void quotaRoundTrip() {
        String pid = os.getToken().getProject().getId();
        int n = adminOnly(() -> os.dns().quotas().get(pid)).get("zones");
        boolean reset;
        try {
            Assert.assertEquals(adminOnly(() -> os.dns().quotas().update(pid, Map.of("zones", n + 1))).get("zones"), Integer.valueOf(n + 1));
            Assert.assertEquals(os.dns().quotas().get(pid).get("zones"), Integer.valueOf(n + 1));
        } finally {
            reset = os.dns().quotas().reset(pid).isSuccess();
        }
        String restoreError = null;
        try {
            if (!Objects.equals(os.dns().quotas().get(pid).get("zones"), n))
                os.dns().quotas().update(pid, Map.of("zones", n));
        } catch (RuntimeException e) {
            restoreError = e.toString();
        }
        Assert.assertTrue(reset, "quota not reset");
        Assert.assertNull(restoreError, "quota not restored to " + n + ": " + restoreError);
    }
}
