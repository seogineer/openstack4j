package org.openstack4j.api.storage.microversion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.storage.block.ReplicationTarget;
import org.openstack4j.model.storage.block.VolumeGroup;
import org.openstack4j.model.storage.block.VolumeGroupSnapshot;
import org.openstack4j.model.storage.block.VolumeGroupType;
import org.openstack4j.model.storage.block.options.GroupCreate;
import org.openstack4j.model.storage.block.options.GroupSnapshotListOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "BlockStorage/Groups")
public class GroupTests extends AbstractBlockStorageMicroVersionTest {

    private static final String GROUP = "6f519a48-3183-46cf-a32f-41815f813986";
    private static final String GROUP_TYPE = "29514915-5208-46ab-9ece-1cc4688ad0c1";
    private static final String GROUP_JSON = "{\"group\": {\"id\": \"" + GROUP + "\", \"status\": \"available\", \"availability_zone\": \"az1\", \"created_at\": \"2026-10-02T09:28:52.000000\","
            + " \"name\": \"first_group\", \"description\": \"my first group\", \"group_type\": \"" + GROUP_TYPE + "\", \"volume_types\": [\"t1\"], \"volumes\": [\"" + VOLUME + "\"],"
            + " \"group_snapshot_id\": null, \"source_group_id\": null, \"project_id\": \"p1\", \"replication_status\": \"enabled\"}}";

    public void groupLifecycle() throws Exception {
        negotiate("3.71");
        respondWith(202, GROUP_JSON);
        respondWith(202, GROUP_JSON);
        respondWith(200, GROUP_JSON);
        respondWith(200, "{\"groups\": [" + GROUP_JSON.substring(10, GROUP_JSON.length() - 1) + "]}");
        respondWith(202, GROUP_JSON);
        respondWith(202);
        respondWith(202);

        VolumeGroup created = osv3().blockStorage().groups().create(GroupCreate.create("first_group", GROUP_TYPE, Arrays.asList("t1")).description("my first group").availabilityZone("az1"));
        VolumeGroup fromSource = osv3().blockStorage().groups().createFromSource("copy", null, null, GROUP);
        VolumeGroup shown = osv3().blockStorage().groups().get(GROUP);
        List<? extends VolumeGroup> all = osv3().blockStorage().groups().listDetail();
        osv3().blockStorage().groups().update(GROUP, "renamed", null, Arrays.asList("v2"), Collections.emptyList());
        osv3().blockStorage().groups().resetStatus(GROUP, "available");
        osv3().blockStorage().groups().delete(GROUP, true);

        JsonNode create = body(takeRequest()).get("group");
        Assert.assertEquals(create.get("group_type").asText(), GROUP_TYPE);
        Assert.assertEquals(create.get("volume_types").get(0).asText(), "t1");
        Assert.assertEquals(create.get("availability_zone").asText(), "az1");
        RecordedRequest src = takeRequest();
        Assert.assertTrue(src.getPath().endsWith("/groups/action"));
        JsonNode srcBody = body(src).get("create-from-src");
        Assert.assertEquals(srcBody.get("source_group_id").asText(), GROUP);
        Assert.assertFalse(srcBody.has("group_snapshot_id"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/groups/" + GROUP));
        Assert.assertTrue(takeRequest().getPath().endsWith("/groups/detail"));
        RecordedRequest update = takeRequest();
        Assert.assertEquals(update.getMethod(), "PUT");
        JsonNode upd = body(update).get("group");
        Assert.assertEquals(upd.get("name").asText(), "renamed");
        Assert.assertEquals(upd.get("add_volumes").asText(), "v2");
        Assert.assertEquals(upd.get("remove_volumes").asText(), "");
        Assert.assertEquals(body(takeRequest()).get("reset_status").get("status").asText(), "available");
        RecordedRequest delete = takeRequest();
        Assert.assertTrue(delete.getPath().endsWith("/groups/" + GROUP + "/action"));
        Assert.assertTrue(body(delete).get("delete").get("delete-volumes").asBoolean());
        Assert.assertEquals(created.getVolumeTypes(), Arrays.asList("t1"));
        Assert.assertEquals(created.getVolumes(), Arrays.asList(VOLUME));
        Assert.assertEquals(created.getProjectId(), "p1");
        Assert.assertEquals(fromSource.getId(), GROUP);
        Assert.assertEquals(shown.getReplicationStatus(), "enabled");
        Assert.assertEquals(all.size(), 1);
    }

    public void groupReplicationActions() throws Exception {
        negotiate("3.71");
        respondWith(202);
        respondWith(202);
        respondWith(202);
        respondWith(200, "{\"replication_targets\": [{\"backend_id\": \"vendor-id-1\", \"unique_key\": \"value1\"}]}");

        osv3().blockStorage().groups().enableReplication(GROUP);
        osv3().blockStorage().groups().disableReplication(GROUP);
        osv3().blockStorage().groups().failoverReplication(GROUP, true, "vendor-id-1");
        List<? extends ReplicationTarget> targets = osv3().blockStorage().groups().listReplicationTargets(GROUP);

        Assert.assertTrue(body(takeRequest()).has("enable_replication"));
        Assert.assertTrue(body(takeRequest()).has("disable_replication"));
        JsonNode failover = body(takeRequest()).get("failover_replication");
        Assert.assertTrue(failover.get("allow_attached_volume").asBoolean());
        Assert.assertEquals(failover.get("secondary_backend_id").asText(), "vendor-id-1");
        Assert.assertTrue(body(takeRequest()).has("list_replication_targets"));
        Assert.assertEquals(targets.get(0).getBackendId(), "vendor-id-1");
        Assert.assertEquals(targets.get(0).getProperties().get("unique_key"), "value1");
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.38.*")
    public void replicationNeeds338() throws Exception {
        negotiate("3.71");
        osv3().blockStorage().microVersions().use("3.30");
        try {
            osv3().blockStorage().groups().enableReplication(GROUP);
        } finally {
            assertNoMoreRequests();
        }
    }

    @Test(expectedExceptions = MicroVersionException.class, expectedExceptionsMessageRegExp = ".*3\\.13.*")
    public void groupsNeed313() throws Exception {
        try {
            osv3().blockStorage().groups().list();
        } finally {
            assertNoMoreRequests();
        }
    }

    public void groupTypesAndSpecs() throws Exception {
        negotiate("3.71");
        String type = "{\"group_type\": {\"id\": \"" + GROUP_TYPE + "\", \"name\": \"grp-type-001\", \"description\": \"d\", \"is_public\": true, \"group_specs\": {\"consistent_group_snapshot_enabled\": \"<is> False\"}}}";
        respondWith(200, "{\"group_types\": [" + type.substring(15, type.length() - 1) + "]}");
        respondWith(200, type);
        respondWith(202, type);
        respondWith(200, type);
        respondWith(202);
        respondWith(200, "{\"group_specs\": {\"key1\": \"value1\"}}");
        respondWith(202, "{\"group_specs\": {\"key1\": \"value1\", \"key2\": \"value2\"}}");
        respondWith(200, "{\"key1\": \"value1\"}");
        respondWith(200, "{\"key1\": \"v\"}");
        respondWith(202);

        List<? extends VolumeGroupType> types = osv3().blockStorage().groupTypes().list();
        VolumeGroupType def = osv3().blockStorage().groupTypes().getDefault();
        VolumeGroupType created = osv3().blockStorage().groupTypes().create("grp-type-001", "d", true, Collections.singletonMap("consistent_group_snapshot_enabled", "<is> False"));
        osv3().blockStorage().groupTypes().update(GROUP_TYPE, "renamed", null, null);
        osv3().blockStorage().groupTypes().delete(GROUP_TYPE);
        osv3().blockStorage().groupTypes().groupSpecs(GROUP_TYPE);
        osv3().blockStorage().groupTypes().setGroupSpecs(GROUP_TYPE, Collections.singletonMap("key2", "value2"));
        String one = osv3().blockStorage().groupTypes().groupSpec(GROUP_TYPE, "key1");
        String updated = osv3().blockStorage().groupTypes().updateGroupSpec(GROUP_TYPE, "key1", "v");
        osv3().blockStorage().groupTypes().deleteGroupSpec(GROUP_TYPE, "key1");

        Assert.assertTrue(takeRequest().getPath().endsWith("/group_types"));
        Assert.assertTrue(takeRequest().getPath().endsWith("/group_types/default"));
        JsonNode create = body(takeRequest()).get("group_type");
        Assert.assertTrue(create.get("is_public").asBoolean());
        Assert.assertEquals(create.get("group_specs").get("consistent_group_snapshot_enabled").asText(), "<is> False");
        Assert.assertEquals(body(takeRequest()).get("group_type").get("name").asText(), "renamed");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertTrue(takeRequest().getPath().endsWith("/group_types/" + GROUP_TYPE + "/group_specs"));
        Assert.assertEquals(body(takeRequest()).get("group_specs").get("key2").asText(), "value2");
        Assert.assertTrue(takeRequest().getPath().endsWith("/group_specs/key1"));
        Assert.assertEquals(body(takeRequest()).get("key1").asText(), "v");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(types.get(0).getName(), "grp-type-001");
        Assert.assertEquals(def.isPublic(), Boolean.TRUE);
        Assert.assertEquals(created.getGroupSpecs().size(), 1);
        Assert.assertEquals(one, "value1");
        Assert.assertEquals(updated, "v");
    }

    public void groupSnapshots() throws Exception {
        negotiate("3.71");
        String snap = "{\"group_snapshot\": {\"id\": \"gs1\", \"group_id\": \"" + GROUP + "\", \"status\": \"available\", \"created_at\": \"2026-10-02T09:28:52.000000\", \"name\": \"my_group_snapshot1\","
                + " \"description\": \"d\", \"group_type_id\": \"" + GROUP_TYPE + "\", \"project_id\": \"p1\"}}";
        respondWith(202, snap);
        respondWith(200, "{\"group_snapshots\": [" + snap.substring(19, snap.length() - 1) + "]}");
        respondWith(200, snap);
        respondWith(202);
        respondWith(202);

        VolumeGroupSnapshot created = osv3().blockStorage().groupSnapshots().create(GROUP, "my_group_snapshot1", "d");
        List<? extends VolumeGroupSnapshot> all = osv3().blockStorage().groupSnapshots().listDetail(GroupSnapshotListOptions.create().groupId(GROUP).limit(5));
        VolumeGroupSnapshot shown = osv3().blockStorage().groupSnapshots().get("gs1");
        osv3().blockStorage().groupSnapshots().resetStatus("gs1", "available");
        osv3().blockStorage().groupSnapshots().delete("gs1");

        Assert.assertEquals(body(takeRequest()).get("group_snapshot").get("group_id").asText(), GROUP);
        String list = takeRequest().getPath();
        Assert.assertTrue(list.contains("/group_snapshots/detail?") && list.contains("group_id=" + GROUP), list);
        Assert.assertTrue(takeRequest().getPath().endsWith("/group_snapshots/gs1"));
        Assert.assertEquals(body(takeRequest()).get("reset_status").get("status").asText(), "available");
        Assert.assertEquals(takeRequest().getMethod(), "DELETE");
        Assert.assertEquals(created.getGroupTypeId(), GROUP_TYPE);
        Assert.assertEquals(all.get(0).getProjectId(), "p1");
        Assert.assertEquals(shown.getName(), "my_group_snapshot1");
        Assert.assertEquals(GroupSnapshotListOptions.create().limit(1).getRequiredMicroVersion(), "3.29");
    }
}
