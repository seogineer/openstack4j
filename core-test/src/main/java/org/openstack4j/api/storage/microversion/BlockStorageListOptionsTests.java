package org.openstack4j.api.storage.microversion;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.Builders;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.VolumeSummary;
import org.openstack4j.model.storage.block.options.BackupListOptions;
import org.openstack4j.model.storage.block.options.SnapshotListOptions;
import org.openstack4j.model.storage.block.options.UploadImageData;
import org.openstack4j.model.storage.block.options.VolumeListOptions;
import org.openstack4j.model.storage.block.options.VolumeUpdateOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/ListOptions")
public class BlockStorageListOptionsTests extends AbstractBlockStorageMicroVersionTest {

    /** Connectors encode query strings differently ({@code ~} vs {@code %7E}, {@code +} vs {@code %20}); compare decoded. */
    private static String decoded(RecordedRequest request) {
        return java.net.URLDecoder.decode(request.getPath(), java.nio.charset.StandardCharsets.UTF_8);
    }

    public void volumeListOptionsBecomeQuery() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"volumes\": [], \"count\": 0}");

        osv3().blockStorage().volumes().list(VolumeListOptions.create().status("available").nameLike("web").withCount(true)
                .createdAt("gt", "2026-10-01T00:00:00").consumesQuota(true).limit(5).sortKey("created_at").sortDir("desc").allTenants(true));

        RecordedRequest request = takeRequest();
        assertVersionHeader(request, "3.71");
        String path = decoded(request);
        Assert.assertTrue(path.matches("/v3/\\p{XDigit}+/volumes/detail\\?.*"), path);
        for (String part : new String[] {"status=available", "name~=web", "with_count=true", "created_at=gt:2026-10-01T00:00:00",
                "consumes_quota=true", "limit=5", "sort_key=created_at", "sort_dir=desc", "all_tenants=true"})
            Assert.assertTrue(path.contains(part), path + " lacks " + part);
        Assert.assertEquals(VolumeListOptions.create().withCount(true).getRequiredMicroVersion(), "3.45");
        Assert.assertEquals(VolumeListOptions.create().nameLike("x").getRequiredMicroVersion(), "3.34");
        Assert.assertNull(VolumeListOptions.create().status("x").limit(1).getRequiredMicroVersion());
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.65.*")
    public void volumeListFloorIsChecked() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.60");
        try {
            osv3().blockStorage().volumes().list(VolumeListOptions.create().consumesQuota(false));
        } finally {
            assertNoMoreRequests();
        }
    }

    public void snapshotAndBackupListOptions() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"snapshots\": []}");
        respondWith(200, "{\"backups\": []}");

        osv3().blockStorage().snapshots().listDetail(SnapshotListOptions.create().volumeId(VOLUME).metadata(Collections.singletonMap("k", "v")));
        osv3().blockStorage().backups().list(BackupListOptions.create().status("available").sortKey("name"));

        String snapshots = decoded(takeRequest());
        Assert.assertTrue(snapshots.contains("/snapshots/detail?"), snapshots);
        Assert.assertTrue(snapshots.contains("volume_id=" + VOLUME), snapshots);
        Assert.assertTrue(snapshots.contains("metadata={'k': 'v'}"), snapshots);   // Cinder's dict-style filter
        String backups = takeRequest().getPath();
        Assert.assertTrue(backups.contains("/backups/detail?") && backups.contains("sort_key=name"), backups);
        Assert.assertEquals(SnapshotListOptions.create().metadata(Map.of("a", "b")).getRequiredMicroVersion(), "3.22");
    }

    public void volumeSummaryWithAndWithoutOptions() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"volume-summary\": {\"total_size\": 4, \"total_count\": 4, \"metadata\": {\"key1\": [\"value1\", \"value2\"]}}}");
        respondWith(200, "{\"volume-summary\": {\"total_size\": 0, \"total_count\": 0, \"metadata\": {}}}");

        VolumeSummary summary = osv3().blockStorage().volumes().summary();
        osv3().blockStorage().volumes().summary(VolumeListOptions.create().allTenants(true));

        Assert.assertTrue(takeRequest().getPath().endsWith("/volumes/summary"));
        Assert.assertTrue(takeRequest().getPath().contains("/volumes/summary?all_tenants=true"));
        Assert.assertEquals(summary.getTotalCount(), Long.valueOf(4));
        Assert.assertEquals(summary.getMetadata().get("key1").get(1), "value2");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.12.*")
    public void summaryNeeds312() throws Exception {
        try {
            osv3().blockStorage().volumes().summary();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void createWithGroupAndBackupAndSchedulerHints() throws Exception {
        negotiate("3.71");
        respondWith(202, "{\"volume\": {\"id\": \"" + VOLUME + "\", \"status\": \"creating\"}}");

        osv3().blockStorage().volumes().create(Builders.volume().name("v").size(1).groupId("g1").backupId("b1").imageId("img")
                .schedulerHints(Collections.singletonMap("same_host", "x")).build());

        RecordedRequest request = takeRequest();
        assertVersionHeader(request, "3.71");
        JsonNode body = body(request);
        Assert.assertEquals(body.get("volume").get("group_id").asText(), "g1");
        Assert.assertEquals(body.get("volume").get("backup_id").asText(), "b1");
        Assert.assertEquals(body.get("volume").get("image_id").asText(), "img");
        Assert.assertEquals(body.get("OS-SCH-HNT:scheduler_hints").get("same_host").asText(), "x");
        Assert.assertFalse(body.get("volume").has("bootable"));
        Assert.assertFalse(body.get("volume").has("scheduler_hints"));
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.47.*")
    public void backupIdNeeds347() throws Exception {
        try {
            osv3().blockStorage().volumes().create(Builders.volume().name("v").size(1).backupId("b1").build());
        } finally {
            assertNoMoreRequests();
        }
    }

    public void backupCreateWithMetadataAndZone() throws Exception {
        negotiate("3.71");
        respondWith(202, "{\"backup\": {\"id\": \"b1\", \"name\": \"n\"}}");

        osv3().blockStorage().backups().create(Builders.volumeBackupCreate().volumeId(VOLUME).name("n")
                .metadata(Collections.singletonMap("k", "v")).availabilityZone("az2").build());

        JsonNode body = body(takeRequest()).get("backup");
        Assert.assertEquals(body.get("metadata").get("k").asText(), "v");
        Assert.assertEquals(body.get("availability_zone").asText(), "az2");
    }

    public void uploadToImageWithVisibility() throws Exception {
        negotiate("3.71");
        respondWith(202, "{\"os-volume_upload_image\": {\"id\": \"" + VOLUME + "\", \"image_id\": \"i\", \"image_name\": \"n\", \"status\": \"uploading\"}}");

        osv3().blockStorage().volumes().uploadToImage(VOLUME, UploadImageData.create("n").visibility("private").protectedImage(true));

        JsonNode body = body(takeRequest()).get("os-volume_upload_image");
        Assert.assertEquals(body.get("visibility").asText(), "private");
        Assert.assertTrue(body.get("protected").asBoolean());
    }

    public void updateWithOptions() throws Exception {
        negotiate("3.71");
        respondWith(200, "{\"volume\": {\"id\": \"" + VOLUME + "\", \"name\": \"renamed\"}}");

        osv3().blockStorage().volumes().update(VOLUME, VolumeUpdateOptions.create().name("renamed").metadata(Collections.singletonMap("k", "v")));

        RecordedRequest request = takeRequest();
        Assert.assertEquals(request.getMethod(), "PUT");
        JsonNode body = body(request).get("volume");
        Assert.assertEquals(body.get("name").asText(), "renamed");
        Assert.assertEquals(body.get("metadata").get("k").asText(), "v");
        Assert.assertFalse(body.has("description"));
    }
}
