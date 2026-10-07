package org.openstack4j.openstack.manila.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.ShareGroupTypeService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareGroupType;
import org.openstack4j.model.manila.ext.options.ShareGroupTypeCreate;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareGroupType;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareGroupType.ManilaShareGroupTypeList;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;

public class ShareGroupTypeServiceImpl extends BaseManilaExtService implements ShareGroupTypeService {

    private static final MicroVersion FLOOR = ManilaMicroVersions.V(55);

    @Override
    public List<? extends ShareGroupType> list() {
        return list(null);
    }

    @Override
    public List<? extends ShareGroupType> list(Map<String, String> filters) {
        return listOf(FLOOR, ManilaShareGroupTypeList.class, "/share-group-types", filters);
    }

    @Override
    public ShareGroupType get(String id) {
        return show(FLOOR, ManilaShareGroupType.class, "/share-group-types/" + id(id));
    }

    @Override
    public ShareGroupType create(ShareGroupTypeCreate create) {
        return create(FLOOR, ManilaShareGroupType.class, "/share-group-types", "share_group_type", create);
    }

    @Override
    public ShareGroupType getDefault() {
        return show(FLOOR, ManilaShareGroupType.class, "/share-group-types/default");
    }

    @Override
    public Map<String, String> getGroupSpecs(String id) {
        return strings(showStrict(FLOOR, Map.class, "/share-group-types/" + id(id) + "/group-specs"), "group_specs");
    }

    @Override
    public Map<String, String> setGroupSpecs(String id, Map<String, String> groupSpecs) {
        String path = "/share-group-types/" + id(id) + "/group-specs";
        return strings(at(FLOOR, post(Map.class, path), path).entity(org.openstack4j.openstack.internal.microversion.JsonBody.of("group_specs", java.util.Objects.requireNonNull(groupSpecs, "groupSpecs"))).execute(propagate404()), "group_specs");
    }

    @Override
    public ActionResponse unsetGroupSpec(String id, String key) {
        return remove(FLOOR, "/share-group-types/" + id(id) + "/group-specs/" + id(key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> listAccess(String id) {
        Map<String, Object> body = showStrict(FLOOR, Map.class, "/share-group-types/" + id(id) + "/access");
        Object access = body == null ? null : body.get("share_group_type_access");
        return access instanceof List ? (List<Map<String, Object>>) access : java.util.Collections.emptyList();
    }

    @Override
    public ActionResponse addAccess(String id, String projectId) {
        return action(FLOOR, "/share-group-types/" + id(id), "addProjectAccess", Map.of("project", java.util.Objects.requireNonNull(projectId, "projectId")));
    }

    @Override
    public ActionResponse removeAccess(String id, String projectId) {
        return action(FLOOR, "/share-group-types/" + id(id), "removeProjectAccess", Map.of("project", java.util.Objects.requireNonNull(projectId, "projectId")));
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(FLOOR, "/share-group-types/" + id(id));
    }
}
