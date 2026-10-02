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
import org.openstack4j.api.storage.BlockVolumeTypeService;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.VolumeTypeAccess;
import org.openstack4j.model.storage.block.VolumeTypeEncryption;
import org.openstack4j.model.storage.block.options.VolumeTypeListOptions;
import org.openstack4j.openstack.storage.block.domain.CinderVolumeType.VolumeTypes;
import org.openstack4j.openstack.storage.block.domain.CinderVolumeTypeAccess.VolumeTypeAccesses;

public class BlockVolumeTypeServiceImpl extends BaseBlockStorageServices implements BlockVolumeTypeService {

    @Override public List<? extends VolumeType> list() { return get(VolumeTypes.class, uri("/types")).execute().getList(); }

    @Override
    public List<? extends VolumeType> list(VolumeTypeListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Volume type list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(VolumeTypes.class, uri("/types")).params(options.toQueryParams()).execute().getList();
    }

    @Override public VolumeType get(String id) { return get(CinderVolumeType.class, uri("/types/%s", Objects.requireNonNull(id))).execute(); }
    @Override public VolumeType getDefault() { return get(CinderVolumeType.class, uri("/types/default")).execute(); }
    @Override public VolumeType create(VolumeType type) { return post(CinderVolumeType.class, uri("/types")).entity(Objects.requireNonNull(type)).execute(); }

    @Override
    public VolumeType update(String id, String name, String description, Boolean isPublic) {
        Objects.requireNonNull(id);
        Map<String, Object> body = new LinkedHashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (isPublic != null) body.put("is_public", isPublic);
        return put(CinderVolumeType.class, uri("/types/%s", id)).entity(JsonBody.of("volume_type", body)).execute();
    }

    @Override public ActionResponse delete(String id) { return deleteWithResponse(uri("/types/%s", Objects.requireNonNull(id))).execute(); }

    @Override
    public Map<String, String> extraSpecs(String id) {
        CinderExtraSpecs specs = get(CinderExtraSpecs.class, uri("/types/%s/extra_specs", Objects.requireNonNull(id))).execute();
        return specs == null || specs.getExtraSpecs() == null ? Collections.emptyMap() : specs.getExtraSpecs();
    }

    @Override
    public Map<String, String> setExtraSpecs(String id, Map<String, String> specs) {
        return post(CinderExtraSpecs.class, uri("/types/%s/extra_specs", Objects.requireNonNull(id)))
                .entity(JsonBody.of("extra_specs", Objects.requireNonNull(specs))).execute().getExtraSpecs();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String extraSpec(String id, String key) {
        Map<String, String> one = get(HashMap.class, uri("/types/%s/extra_specs/%s", Objects.requireNonNull(id), Objects.requireNonNull(key))).execute();
        return one == null ? null : one.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String updateExtraSpec(String id, String key, String value) {
        Map<String, String> one = put(HashMap.class, uri("/types/%s/extra_specs/%s", Objects.requireNonNull(id), Objects.requireNonNull(key)))
                .entity(JsonBody.of(Collections.singletonMap(key, value))).execute();
        return one == null ? null : one.get(key);
    }

    @Override public ActionResponse deleteExtraSpec(String id, String key) { return deleteWithResponse(uri("/types/%s/extra_specs/%s", id, key)).execute(); }

    @Override
    public ActionResponse addProjectAccess(String id, String projectId) {
        return post(ActionResponse.class, uri("/types/%s/action", Objects.requireNonNull(id)))
                .entity(JsonBody.of("addProjectAccess", Collections.singletonMap("project", Objects.requireNonNull(projectId)))).execute();
    }

    @Override
    public ActionResponse removeProjectAccess(String id, String projectId) {
        return post(ActionResponse.class, uri("/types/%s/action", Objects.requireNonNull(id)))
                .entity(JsonBody.of("removeProjectAccess", Collections.singletonMap("project", Objects.requireNonNull(projectId)))).execute();
    }

    @Override public List<? extends VolumeTypeAccess> listProjectAccess(String id) { return get(VolumeTypeAccesses.class, uri("/types/%s/os-volume-type-access", Objects.requireNonNull(id))).execute().getList(); }
    @Override public VolumeTypeEncryption encryption(String id) { return get(CinderVolumeTypeEncryptionFetch.class, uri("/types/%s/encryption", Objects.requireNonNull(id))).execute(); }

    @Override
    @SuppressWarnings("unchecked")
    public String encryptionSpec(String id, String key) {
        Map<String, Object> one = get(HashMap.class, uri("/types/%s/encryption/%s", Objects.requireNonNull(id), Objects.requireNonNull(key))).execute();
        return one == null || one.get(key) == null ? null : String.valueOf(one.get(key));
    }

    @Override public VolumeTypeEncryption createEncryption(String id, VolumeTypeEncryption encryption) { return post(CinderVolumeTypeEncryption.class, uri("/types/%s/encryption", Objects.requireNonNull(id))).entity(Objects.requireNonNull(encryption)).execute(); }
    @Override public VolumeTypeEncryption updateEncryption(String id, String encryptionId, VolumeTypeEncryption encryption) { return put(CinderVolumeTypeEncryption.class, uri("/types/%s/encryption/%s", Objects.requireNonNull(id), Objects.requireNonNull(encryptionId))).entity(Objects.requireNonNull(encryption)).execute(); }
    @Override public ActionResponse deleteEncryption(String id, String encryptionId) { return deleteWithResponse(uri("/types/%s/encryption/%s", id, encryptionId)).execute(); }
}
