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
import org.openstack4j.api.storage.BlockDefaultTypeService;
import org.openstack4j.model.storage.block.DefaultVolumeType;
import org.openstack4j.openstack.storage.block.domain.CinderDefaultVolumeType.DefaultVolumeTypes;

public class BlockDefaultTypeServiceImpl extends BaseBlockStorageServices implements BlockDefaultTypeService {

    @Override
    public List<? extends DefaultVolumeType> list() {
        requireMicroVersion("Default volume types", V(62));
        return get(DefaultVolumeTypes.class, uri("/default-types")).execute().getList();
    }

    @Override
    public DefaultVolumeType get(String projectId) {
        Objects.requireNonNull(projectId);
        requireMicroVersion("Default volume types", V(62));
        return get(CinderDefaultVolumeType.class, uri("/default-types/%s", projectId)).execute();
    }

    @Override
    public DefaultVolumeType set(String projectId, String volumeType) {
        Objects.requireNonNull(projectId);
        Objects.requireNonNull(volumeType);
        requireMicroVersion("Default volume types", V(62));
        return put(CinderDefaultVolumeType.class, uri("/default-types/%s", projectId))
                .entity(JsonBody.of("default_type", Collections.singletonMap("volume_type", volumeType))).execute();
    }

    @Override
    public ActionResponse unset(String projectId) {
        Objects.requireNonNull(projectId);
        requireMicroVersion("Default volume types", V(62));
        return deleteWithResponse(uri("/default-types/%s", projectId)).execute();
    }
}
