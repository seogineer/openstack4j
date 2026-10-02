package org.openstack4j.openstack.storage.block.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.*;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;
import org.openstack4j.api.storage.BlockManageableVolumeService;
import org.openstack4j.model.storage.block.ManageableVolume;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.options.ManageableListOptions;
import org.openstack4j.model.storage.block.options.VolumeManageRequest;
import org.openstack4j.openstack.storage.block.domain.CinderManageableVolume.ManageableVolumes;

public class BlockManageableVolumeServiceImpl extends BaseBlockStorageServices implements BlockManageableVolumeService {

    @Override
    public List<? extends ManageableVolume> list(ManageableListOptions options) {
        requireOptions(options);
        return get(ManageableVolumes.class, uri("/manageable_volumes")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public List<? extends ManageableVolume> listDetail(ManageableListOptions options) {
        requireOptions(options);
        return get(ManageableVolumes.class, uri("/manageable_volumes/detail")).params(options.toQueryParams()).execute().getList();
    }

    private void requireOptions(ManageableListOptions options) {
        Objects.requireNonNull(options);
        requireMicroVersion("Manageable volumes", V(8));
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Manageable list options " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
    }

    @Override
    public Volume manage(VolumeManageRequest request) {
        Objects.requireNonNull(request);
        requireMicroVersion("Manageable volumes", V(8));
        if (request.getCluster() != null)
            requireMicroVersion("Manage to a cluster", V(16));
        return post(CinderVolume.class, uri("/manageable_volumes")).entity(JsonBody.of("volume", request.toMap())).execute();
    }
}
