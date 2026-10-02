package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.*;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;
import org.openstack4j.api.storage.BlockGroupTypeService;
import org.openstack4j.model.storage.block.VolumeGroupType;
import org.openstack4j.openstack.storage.block.domain.CinderGroupType.GroupTypes;

public class BlockGroupTypeServiceImpl extends BaseBlockStorageServices implements BlockGroupTypeService {

    @Override
    public List<? extends VolumeGroupType> list() {
        requireMicroVersion("Group types", V(11));
        return get(GroupTypes.class, uri("/group_types")).execute().getList();
    }

    @Override
    public VolumeGroupType get(String groupTypeId) {
        requireMicroVersion("Group types", V(11));
        return get(CinderGroupType.class, uri("/group_types/%s", Objects.requireNonNull(groupTypeId))).execute();
    }

    @Override
    public VolumeGroupType getDefault() {
        requireMicroVersion("Group types", V(11));
        return get(CinderGroupType.class, uri("/group_types/default")).execute();
    }

    @Override
    public VolumeGroupType create(String name, String description, Boolean isPublic, Map<String, String> groupSpecs) {
        Objects.requireNonNull(name);
        requireMicroVersion("Group types", V(11));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        if (description != null) body.put("description", description);
        if (isPublic != null) body.put("is_public", isPublic);
        if (groupSpecs != null) body.put("group_specs", groupSpecs);
        return post(CinderGroupType.class, uri("/group_types")).entity(JsonBody.of("group_type", body)).execute();
    }

    @Override
    public VolumeGroupType update(String groupTypeId, String name, String description, Boolean isPublic) {
        Objects.requireNonNull(groupTypeId);
        requireMicroVersion("Group types", V(11));
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (isPublic != null) body.put("is_public", isPublic);
        return put(CinderGroupType.class, uri("/group_types/%s", groupTypeId)).entity(JsonBody.of("group_type", body)).execute();
    }

    @Override
    public ActionResponse delete(String groupTypeId) {
        requireMicroVersion("Group types", V(11));
        return deleteWithResponse(uri("/group_types/%s", Objects.requireNonNull(groupTypeId))).execute();
    }

    @Override
    public Map<String, String> groupSpecs(String groupTypeId) {
        requireMicroVersion("Group types", V(11));
        CinderGroupSpecs specs = get(CinderGroupSpecs.class, uri("/group_types/%s/group_specs", Objects.requireNonNull(groupTypeId))).execute();
        return specs == null || specs.getGroupSpecs() == null ? Collections.emptyMap() : specs.getGroupSpecs();
    }

    @Override
    public Map<String, String> setGroupSpecs(String groupTypeId, Map<String, String> groupSpecs) {
        requireMicroVersion("Group types", V(11));
        return post(CinderGroupSpecs.class, uri("/group_types/%s/group_specs", Objects.requireNonNull(groupTypeId)))
                .entity(JsonBody.of("group_specs", Objects.requireNonNull(groupSpecs))).execute().getGroupSpecs();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String groupSpec(String groupTypeId, String key) {
        requireMicroVersion("Group types", V(11));
        Map<String, String> one = get(HashMap.class, uri("/group_types/%s/group_specs/%s", Objects.requireNonNull(groupTypeId), Objects.requireNonNull(key))).execute();
        return one == null ? null : one.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String updateGroupSpec(String groupTypeId, String key, String value) {
        requireMicroVersion("Group types", V(11));
        Map<String, String> one = put(HashMap.class, uri("/group_types/%s/group_specs/%s", Objects.requireNonNull(groupTypeId), Objects.requireNonNull(key)))
                .entity(JsonBody.of(Collections.singletonMap(key, value))).execute();
        return one == null ? null : one.get(key);
    }

    @Override
    public ActionResponse deleteGroupSpec(String groupTypeId, String key) {
        requireMicroVersion("Group types", V(11));
        return deleteWithResponse(uri("/group_types/%s/group_specs/%s", Objects.requireNonNull(groupTypeId), Objects.requireNonNull(key))).execute();
    }
}
