package org.openstack4j.api.barbican;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.exceptions.ResponseException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.model.barbican.Container;
import org.openstack4j.model.barbican.ContainerSecret;
import org.openstack4j.model.barbican.ext.Order;
import org.openstack4j.model.barbican.ext.SecretStore;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Runs against a real Barbican when OS_AUTH_URL is set; every temporary resource is removed in finally. */
@Test(suiteName = "Barbican/Live", groups = "barbican-live", singleThreaded = true)
public class BarbicanLiveTests {

    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live barbican tests");
        String authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        String token = System.getenv("OS_TOKEN");
        Identifier project = Identifier.byName(env("OS_PROJECT_NAME", null));
        Identifier projectDomain = Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"));
        os = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToProject(project, projectDomain).authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null),
                Identifier.byName(env("OS_USER_DOMAIN_NAME", "Default"))).scopeToProject(project, projectDomain).authenticate();
        requireService(ServiceType.BARBICAN);
    }

    private void requireService(ServiceType type) {
        if (os.getToken().getCatalog().stream().noneMatch(s -> ServiceType.forCatalogEntry(s.getType(), s.getName()) == type))
            throw new SkipException(type + " not in the service catalog");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live barbican tests");
        }
        return value;
    }

    /** Unique per run so a run that died before its cleanup cannot collide with the next one. */
    private static final String SUFFIX = Long.toString(System.currentTimeMillis(), 36);

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

    /** The core secrets()/containers() calls take the bare id, the references Barbican returns are URLs. */
    private static String idOf(String ref) {
        String r = ref.replaceAll("/+$", "");
        return r.substring(r.lastIndexOf('/') + 1);
    }

    private Set<String> secretNames(String containerId) {
        return os.barbican().containers().get(containerId).getSecretReferences().stream()
                .map(ContainerSecret::getName).collect(Collectors.toSet());
    }

    public void quotas() {
        Map<String, Integer> quotas = os.barbican().quotas().effective();
        Assert.assertTrue(quotas.containsKey("secrets"), "effective quotas: " + quotas);
    }

    /** Without multiple secret store backends these answer 404; that is the environment. */
    public void secretStores() {
        List<?> stores = skipOn(404, "multiple secret store backends not enabled", () -> os.barbican().secretStores().list());
        Assert.assertFalse(stores.isEmpty());
        SecretStore global = skipOn(404, "multiple secret store backends not enabled", () -> os.barbican().secretStores().globalDefault());
        Assert.assertNotNull(global);
        Assert.assertEquals(global.isGlobalDefault(), Boolean.TRUE);
    }

    public void secretLifecycle() {
        String name = "os4j-live-secret-" + SUFFIX;
        String ref = os.barbican().secrets().create(Builders.secret().name(name).payload("os4j-payload")
                .payloadContentType("text/plain").secretType("opaque").build()).getSecretReference();
        Assert.assertNotNull(ref);
        String id = idOf(ref);
        boolean deleted;
        try {
            Assert.assertEquals(os.barbican().secrets().get(id).getName(), name);
            Assert.assertEquals(os.barbican().secrets().get(id).getStatus(), "ACTIVE");
            Assert.assertTrue(os.barbican().secrets().list(name).stream().anyMatch(s -> id.equals(idOf(s.getSecretReference()))));
            Assert.assertEquals(os.barbican().secretsExt().getTextPayload(ref), "os4j-payload");

            Assert.assertTrue(os.barbican().secretsExt().addMetadataItem(ref, "k", "v").isSuccess());
            Assert.assertEquals(os.barbican().secretsExt().getMetadataItem(ref, "k"), "v");
            Assert.assertTrue(os.barbican().secretsExt().updateMetadataItem(ref, "k", "v2").isSuccess());
            Assert.assertEquals(os.barbican().secretsExt().getMetadata(ref).get("k"), "v2");
            Assert.assertTrue(os.barbican().secretsExt().deleteMetadataItem(ref, "k").isSuccess());
            Assert.assertFalse(os.barbican().secretsExt().getMetadata(ref).containsKey("k"));

            String userId = os.getToken().getUser().getId();
            Assert.assertTrue(os.barbican().acls().setSecretAcl(ref, List.of(userId), false).isSuccess());
            Assert.assertTrue(os.barbican().acls().getSecretAcl(ref).getUsers().contains(userId));
            Assert.assertEquals(os.barbican().acls().getSecretAcl(ref).isProjectAccess(), Boolean.FALSE);
            Assert.assertTrue(os.barbican().acls().deleteSecretAcl(ref).isSuccess());
        } finally {
            deleted = os.barbican().secrets().delete(id).isSuccess();
        }
        Assert.assertTrue(deleted, "secret not deleted");
        Assert.assertNull(os.barbican().secrets().get(id));
    }

    public void containerLifecycle() {
        String refA = os.barbican().secrets().create(Builders.secret().name("os4j-live-ctr-a-" + SUFFIX).payload("a")
                .payloadContentType("text/plain").build()).getSecretReference();
        boolean aDeleted;
        try {
            String refB = os.barbican().secrets().create(Builders.secret().name("os4j-live-ctr-b-" + SUFFIX).payload("b")
                    .payloadContentType("text/plain").build()).getSecretReference();
            boolean bDeleted;
            try {
                Container created = os.barbican().containers().create(Builders.container().name("os4j-live-ctr-" + SUFFIX).type("generic")
                        .secretReferences(List.of(Builders.containerSecret().name("a").reference(refA).build())).build());
                String cRef = created.getContainerReference();
                String cid = idOf(cRef);
                boolean cDeleted;
                try {
                    Container c = os.barbican().containers().get(cid);
                    Assert.assertEquals(c.getType(), "generic");
                    Assert.assertEquals(secretNames(cid), Set.of("a"));

                    Assert.assertTrue(os.barbican().containersExt().addSecret(cRef, "b", refB).isSuccess());
                    Assert.assertEquals(secretNames(cid), new HashSet<>(Arrays.asList("a", "b")));
                    boolean removed = os.barbican().containersExt().removeSecret(cRef, "b", refB).isSuccess();
                    Assert.assertTrue(removed, "secret not removed from container");
                    Assert.assertEquals(secretNames(cid), Set.of("a"));

                    String consumerUrl = "http://os4j.invalid/" + SUFFIX;
                    Assert.assertTrue(os.barbican().containersExt().registerConsumer(cRef, "os4j-live", consumerUrl).isSuccess());
                    boolean consumerRemoved;
                    try {
                        Assert.assertEquals(os.barbican().containersExt().listConsumers(cRef).stream()
                                .filter(m -> consumerUrl.equals(m.get("URL"))).count(), 1);
                    } finally {
                        consumerRemoved = os.barbican().containersExt().removeConsumer(cRef, "os4j-live", consumerUrl).isSuccess();
                    }
                    Assert.assertTrue(consumerRemoved, "consumer not removed");
                    Assert.assertTrue(os.barbican().containersExt().listConsumers(cRef).stream().noneMatch(m -> consumerUrl.equals(m.get("URL"))));
                } finally {
                    cDeleted = os.barbican().containers().delete(cid).isSuccess();
                }
                Assert.assertTrue(cDeleted, "container not deleted");
            } finally {
                bDeleted = os.barbican().secrets().delete(idOf(refB)).isSuccess();
            }
            Assert.assertTrue(bDeleted, "secret b not deleted");
        } finally {
            aDeleted = os.barbican().secrets().delete(idOf(refA)).isSuccess();
        }
        Assert.assertTrue(aDeleted, "secret a not deleted");
    }

    public void keyOrder() throws InterruptedException {
        String orderRef = os.barbican().orders().create("key", Map.of("name", "os4j-live-order-" + SUFFIX, "algorithm", "aes",
                "bit_length", 256, "mode", "cbc", "payload_content_type", "application/octet-stream"));
        Assert.assertNotNull(orderRef);
        String secretRef = null;
        boolean orderDeleted;
        boolean secretDeleted = true;
        try {
            Order order = waitUntil("order ACTIVE", 15, () -> os.barbican().orders().get(orderRef),
                    o -> "ACTIVE".equals(o.getStatus()), o -> "ERROR".equals(o.getStatus()));
            secretRef = order.getSecretRef();
            Assert.assertNotNull(secretRef);
            Assert.assertTrue("aes".equalsIgnoreCase(os.barbican().secrets().get(idOf(secretRef)).getAlgorithm()));
            Assert.assertEquals(os.barbican().secrets().get(idOf(secretRef)).getBitLength(), Integer.valueOf(256));
        } finally {
            orderDeleted = os.barbican().orders().delete(orderRef).isSuccess();
            if (secretRef != null)
                secretDeleted = os.barbican().secrets().delete(idOf(secretRef)).isSuccess();
        }
        Assert.assertTrue(orderDeleted, "order not deleted");
        Assert.assertTrue(secretDeleted, "generated secret not deleted");
    }

    public void twoStepPayload() {
        String ref = os.barbican().secrets().create(Builders.secret().name("os4j-live-two-step-" + SUFFIX).build()).getSecretReference();
        boolean deleted;
        try {
            byte[] payload = {0, 1, 2, (byte) 0xff};
            Assert.assertTrue(os.barbican().secretsExt().storeBinaryPayload(ref, payload).isSuccess());
            Assert.assertEquals(os.barbican().secretsExt().getBinaryPayload(ref), payload);
        } finally {
            deleted = os.barbican().secrets().delete(idOf(ref)).isSuccess();
        }
        Assert.assertTrue(deleted, "secret not deleted");
    }

    /** Project quotas are admin-only; the original value (or its absence) is put back. */
    public void projectQuotaRoundTrip() {
        String pid = os.getToken().getProject().getId();
        Map<String, Integer> original;
        try {
            original = adminOnly(() -> os.barbican().quotas().getProjectQuotas(pid));
        } catch (ResponseException e) {
            if (e.getStatus() != 404) throw e;
            original = null;
        }
        boolean restored;
        try {
            Assert.assertTrue(os.barbican().quotas().setProjectQuotas(pid, Map.of("secrets", 500)).isSuccess());
            Assert.assertEquals(os.barbican().quotas().getProjectQuotas(pid).get("secrets"), Integer.valueOf(500));
        } finally {
            restored = original == null
                    ? os.barbican().quotas().deleteProjectQuotas(pid).isSuccess()
                    : os.barbican().quotas().setProjectQuotas(pid, original).isSuccess();
        }
        Assert.assertTrue(restored, "project quota not restored");
    }
}
