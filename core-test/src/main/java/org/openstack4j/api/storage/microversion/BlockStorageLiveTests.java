package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.BlockStorageVersion;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeAttachmentRecord;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.VolumeSummary;
import org.openstack4j.openstack.OSFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Runs against a real OpenStack when the standard OS_* environment variables are set; skipped otherwise.
 * Negotiates block storage microversions, reads existing resources and creates a throw-away volume, snapshot and
 * attachment that are deleted in {@code finally}.
 */
@Test(suiteName = "BlockStorage/Live", groups = "block-storage-live")
public class BlockStorageLiveTests {

    private OSClientV3 os;

    @BeforeClass
    public void connect() {
        String url = System.getenv("OS_AUTH_URL");
        if (url == null || url.isEmpty())
            throw new SkipException("OS_AUTH_URL not set; skipping live block storage tests");
        String authUrl = url.replaceAll("/+$", "").endsWith("/v3") ? url.replaceAll("/+$", "") : url.replaceAll("/+$", "") + "/v3";
        String token = System.getenv("OS_TOKEN");
        Identifier project = Identifier.byName(env("OS_PROJECT_NAME", null));
        Identifier projectDomain = Identifier.byName(env("OS_PROJECT_DOMAIN_NAME", "Default"));
        os = token != null && !token.isEmpty()
                ? OSFactory.builderV3().endpoint(authUrl).token(token).scopeToProject(project, projectDomain).authenticate()
                : OSFactory.builderV3().endpoint(authUrl).credentials(env("OS_USERNAME", null), env("OS_PASSWORD", null),
                Identifier.byName(env("OS_USER_DOMAIN_NAME", "Default"))).scopeToProject(project, projectDomain).authenticate();
        BlockStorageVersion version = os.blockStorage().microVersions().negotiate();
        Assert.assertTrue(version.isEnabled());
        System.out.println("block storage microversion " + version.getMicroVersion() + " (server max " + version.getServerMaxVersion() + ")");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            if (fallback != null) return fallback;
            throw new SkipException(name + " not set; skipping live block storage tests");
        }
        return value;
    }

    public void readOnlyAdminViews() {
        Assert.assertFalse(os.blockStorage().volumeTypes().list().isEmpty());
        Assert.assertNotNull(os.blockStorage().volumeTypes().getDefault().getId());
        Assert.assertFalse(os.blockStorage().resourceFilters().list().isEmpty());
        Assert.assertFalse(os.blockStorage().schedulerStatsPools().poolsDetail().isEmpty());
        Assert.assertFalse(os.blockStorage().services().list().isEmpty());
        Assert.assertNotNull(os.blockStorage().getLimits().getAbsolute());
        os.blockStorage().clusters().list();
        os.blockStorage().messages().list();
        os.blockStorage().hosts().list();
        os.blockStorage().extensions().list();
        VolumeSummary summary = os.blockStorage().volumes().summary();
        Assert.assertNotNull(summary.getTotalCount());
    }

    /** Resends the current volumes quota unchanged, so nothing is modified and nothing needs cleaning up. */
    public void quotaToBuilderKeepsId() {
        String projectId = os.getToken().getProject().getId();
        BlockQuotaSet q = os.blockStorage().quotaSets().get(projectId);
        int orig = q.getVolumes();
        BlockQuotaSet updated = os.blockStorage().quotaSets().updateForTenant(projectId, q.toBuilder().volumes(orig).build());
        Assert.assertEquals(q.getId(), projectId);
        Assert.assertEquals(updated.getVolumes(), orig);
        // the same object a second time
        updated = os.blockStorage().quotaSets().updateForTenant(projectId, q.toBuilder().volumes(orig).build());
        Assert.assertEquals(q.getId(), projectId);
        Assert.assertEquals(updated.getVolumes(), orig);
    }

    public void legacyCallsStillWorkAfterNegotiation() {
        os.blockStorage().volumes().list();
        os.blockStorage().snapshots().list();
        os.blockStorage().backups().list();
        os.blockStorage().volumes().transfer().list();
        os.blockStorage().volumes().listVolumeTypes();
        os.blockStorage().zones().list();
    }

    public void temporaryVolumeLifecycle() throws Exception {
        String name = "os4j-live-" + UUID.randomUUID().toString().substring(0, 8);
        Volume created = os.blockStorage().volumes().create(Builders.volume().name(name).size(1).description("os4j live test")
                .metadata(Collections.singletonMap("purpose", "live-test")).build());
        final String volumeId = created.getId();
        try {
            waitFor(() -> os.blockStorage().volumes().get(volumeId).getStatus() == Volume.Status.AVAILABLE, "volume available");
            Volume shown = os.blockStorage().volumes().get(volumeId);
            Assert.assertEquals(shown.getConsumesQuota(), Boolean.TRUE);
            Assert.assertNotNull(shown.getVolumeTypeId());
            Map<String, String> metadata = os.blockStorage().volumes().setMetadata(volumeId, Collections.singletonMap("k", "v"));
            Assert.assertEquals(metadata.get("k"), "v");
            Assert.assertEquals(os.blockStorage().volumes().metadataItem(volumeId, "purpose"), "live-test");

            VolumeSnapshot snapshot = os.blockStorage().snapshots().create(Builders.volumeSnapshot().name(name).volume(volumeId).build());
            final String snapshotId = snapshot.getId();
            try {
                waitFor(() -> os.blockStorage().snapshots().get(snapshotId).getStatus() == Volume.Status.AVAILABLE, "snapshot available");
                Assert.assertNotNull(os.blockStorage().snapshots().get(snapshotId).getUserId());
                Assert.assertTrue(os.blockStorage().snapshots().metadata(snapshotId).isEmpty());

                VolumeAttachmentRecord attachment = os.blockStorage().attachments().create(volumeId, null, null, null);
                try {
                    Assert.assertEquals(attachment.getStatus(), "reserved");
                    Assert.assertEquals(os.blockStorage().attachments().get(attachment.getId()).getVolumeId(), volumeId);
                } finally {
                    os.blockStorage().attachments().delete(attachment.getId());
                }
                waitFor(() -> os.blockStorage().volumes().get(volumeId).getStatus() == Volume.Status.AVAILABLE, "volume available after detach");
            } finally {
                os.blockStorage().snapshots().delete(snapshotId);
                waitForGone(() -> os.blockStorage().snapshots().get(snapshotId));
            }
        } finally {
            os.blockStorage().volumes().delete(volumeId);
            waitForGone(() -> os.blockStorage().volumes().get(volumeId));
        }
    }

    /** error_* statuses are sent as Cinder literals (error_extending), not error-extending which Cinder rejects with 400. */
    public void resetStateToCinderErrorLiteral() throws Exception {
        String name = "os4j-live-" + UUID.randomUUID().toString().substring(0, 8);
        final String volumeId = os.blockStorage().volumes().create(Builders.volume().name(name).size(1).build()).getId();
        try {
            waitFor(() -> os.blockStorage().volumes().get(volumeId).getStatus() == Volume.Status.AVAILABLE, "volume available");
            Assert.assertTrue(os.blockStorage().volumes().resetState(volumeId, Volume.Status.ERROR_EXTENDING).isSuccess());
            Assert.assertEquals(os.blockStorage().volumes().get(volumeId).getStatus(), Volume.Status.ERROR_EXTENDING);
            Assert.assertTrue(os.blockStorage().volumes().resetState(volumeId, Volume.Status.AVAILABLE).isSuccess());
            Assert.assertEquals(os.blockStorage().volumes().get(volumeId).getStatus(), Volume.Status.AVAILABLE);
        } finally {
            try {
                os.blockStorage().volumes().resetState(volumeId, Volume.Status.AVAILABLE);
            } catch (RuntimeException ignored) {
                // best effort so that the delete below is accepted
            }
            os.blockStorage().volumes().delete(volumeId);
            waitForGone(() -> os.blockStorage().volumes().get(volumeId));
        }
    }

    private static void waitFor(java.util.function.BooleanSupplier condition, String what) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(2000);
        }
        throw new AssertionError("timed out waiting for " + what);
    }

    private static void waitForGone(java.util.function.Supplier<Object> lookup) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            try {
                if (lookup.get() == null) return;
            } catch (RuntimeException e) {
                return;    // 404
            }
            Thread.sleep(2000);
        }
    }
}
