package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.model.manila.ext.ShareGroup;
import org.openstack4j.model.manila.ext.ShareGroupSnapshot;
import org.openstack4j.model.manila.ext.ShareGroupType;
import org.openstack4j.model.manila.ext.options.ShareGroupCreate;
import org.openstack4j.model.manila.ext.options.ShareGroupSnapshotCreate;
import org.openstack4j.model.manila.ext.options.ShareGroupTypeCreate;
import org.openstack4j.model.manila.ext.options.ShareGroupUpdate;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Manila/ShareGroups")
public class ShareGroupTests extends AbstractManilaExtTest {

    private static final String P = "/v2/b80f8d4e28b74188858b654cb1fccf7d";
    private static final String GROUP = "{\"id\": \"g1\", \"name\": \"my_group\", \"status\": \"creating\", \"share_types\": [\"t1\"], \"share_group_type_id\": \"gt1\","
            + " \"consistent_snapshot_support\": null, \"links\": []}";
    private static final String TYPE = "{\"id\": \"gt1\", \"name\": \"test_group_type\", \"is_public\": true, \"group_specs\": {}, \"share_types\": [\"t1\"], \"is_default\": false}";

    public void groups() throws Exception {
        respondWith(202, "{\"share_group\": " + GROUP + "}");
        respondWith(200, "{\"share_groups\": [" + GROUP + "]}");
        respondWith(200, "{\"share_group\": " + GROUP + "}");
        respondWith(200, "{\"share_group\": " + GROUP + "}");
        respondWith(202);
        respondWith(202);
        respondWith(202);

        var groups = osv3().share().shareGroups();
        ShareGroup created = groups.create(ShareGroupCreate.create().name("my_group").shareTypes(List.of("t1")).shareGroupTypeId("gt1"));
        List<? extends ShareGroup> all = groups.list(Map.of("status", "available"));
        groups.get("g1");
        groups.update("g1", ShareGroupUpdate.create().description("changed"));
        Assert.assertTrue(groups.resetStatus("g1", "error").isSuccess());
        Assert.assertTrue(groups.forceDelete("g1").isSuccess());
        Assert.assertTrue(groups.delete("g1").isSuccess());

        RecordedRequest create = expect("POST", P + "/share-groups");
        Assert.assertEquals(body(create).toString(), "{\"share_group\":{\"name\":\"my_group\",\"share_types\":[\"t1\"],\"share_group_type_id\":\"gt1\"}}");
        Assert.assertEquals(create.getHeader("X-OpenStack-Manila-API-Version"), "2.55");
        expect("GET", P + "/share-groups/detail?status=available");
        expect("GET", P + "/share-groups/g1");
        Assert.assertEquals(body(expect("PUT", P + "/share-groups/g1")).toString(), "{\"share_group\":{\"description\":\"changed\"}}");
        Assert.assertEquals(body(expect("POST", P + "/share-groups/g1/action")).toString(), "{\"reset_status\":{\"status\":\"error\"}}");
        Assert.assertEquals(body(expect("POST", P + "/share-groups/g1/action")).toString(), "{\"force_delete\":null}");
        expect("DELETE", P + "/share-groups/g1");
        Assert.assertEquals(created.getShareTypes(), List.of("t1"));
        Assert.assertEquals(all.get(0).getShareGroupTypeId(), "gt1");
    }

    public void groupSnapshots() throws Exception {
        String snap = "{\"id\": \"gs1\", \"name\": \"test\", \"status\": \"creating\", \"share_group_id\": \"g1\", \"members\": []}";
        respondWith(202, "{\"share_group_snapshot\": " + snap + "}");
        respondWith(200, "{\"share_group_snapshot_members\": [{\"id\": \"m1\", \"share_id\": \"s1\", \"size\": 1, \"status\": \"available\"}]}");
        respondWith(202);

        var snapshots = osv3().share().shareGroupSnapshots();
        ShareGroupSnapshot created = snapshots.create(ShareGroupSnapshotCreate.create("g1").name("test"));
        List<Map<String, Object>> members = snapshots.listMembers("gs1");
        Assert.assertTrue(snapshots.resetStatus("gs1", "available").isSuccess());

        Assert.assertEquals(body(expect("POST", P + "/share-group-snapshots")).toString(), "{\"share_group_snapshot\":{\"share_group_id\":\"g1\",\"name\":\"test\"}}");
        expect("GET", P + "/share-group-snapshots/gs1/members");
        expect("POST", P + "/share-group-snapshots/gs1/action");
        Assert.assertEquals(created.getShareGroupId(), "g1");
        Assert.assertEquals(members.get(0).get("share_id"), "s1");
    }

    public void groupTypes() throws Exception {
        respondWith(200, "{\"share_group_type\": " + TYPE + "}");
        respondWith(200, "{\"share_group_types\": [" + TYPE + "]}");
        respondWith(200, "{\"share_group_type\": " + TYPE.replace("\"is_default\": false", "\"is_default\": true") + "}");
        respondWith(200, "{\"group_specs\": {\"snapshot_support\": \"True\"}}");
        respondWith(200, "{\"group_specs\": {\"k\": \"v\"}}");
        respondWith(204);
        respondWith(200, "{\"share_group_type_access\": [{\"share_group_type_id\": \"gt1\", \"project_id\": \"p2\"}]}");
        respondWith(202);
        respondWith(202);
        respondWith(204);

        var types = osv3().share().shareGroupTypes();
        ShareGroupType created = types.create(ShareGroupTypeCreate.create("test_group_type", List.of("t1")).isPublic(false));
        types.list(Map.of("is_public", "all"));
        ShareGroupType defaultType = types.getDefault();
        Map<String, String> specs = types.getGroupSpecs("gt1");
        types.setGroupSpecs("gt1", Map.of("k", "v"));
        Assert.assertTrue(types.unsetGroupSpec("gt1", "k").isSuccess());
        List<Map<String, Object>> access = types.listAccess("gt1");
        Assert.assertTrue(types.addAccess("gt1", "p2").isSuccess());
        Assert.assertTrue(types.removeAccess("gt1", "p2").isSuccess());
        Assert.assertTrue(types.delete("gt1").isSuccess());

        Assert.assertEquals(body(expect("POST", P + "/share-group-types")).toString(), "{\"share_group_type\":{\"name\":\"test_group_type\",\"share_types\":[\"t1\"],\"is_public\":false}}");
        expect("GET", P + "/share-group-types?is_public=all");
        expect("GET", P + "/share-group-types/default");
        expect("GET", P + "/share-group-types/gt1/group-specs");
        Assert.assertEquals(body(expect("POST", P + "/share-group-types/gt1/group-specs")).toString(), "{\"group_specs\":{\"k\":\"v\"}}");
        expect("DELETE", P + "/share-group-types/gt1/group-specs/k");
        expect("GET", P + "/share-group-types/gt1/access");
        Assert.assertEquals(body(expect("POST", P + "/share-group-types/gt1/action")).toString(), "{\"addProjectAccess\":{\"project\":\"p2\"}}");
        Assert.assertEquals(body(expect("POST", P + "/share-group-types/gt1/action")).toString(), "{\"removeProjectAccess\":{\"project\":\"p2\"}}");
        expect("DELETE", P + "/share-group-types/gt1");
        Assert.assertEquals(created.getName(), "test_group_type");
        Assert.assertEquals(defaultType.isDefault(), Boolean.TRUE);
        Assert.assertEquals(specs.get("snapshot_support"), "True");
        Assert.assertEquals(access.get(0).get("project_id"), "p2");
    }
}
