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
import org.openstack4j.api.storage.BlockVolumeTransferV3Service;
import org.openstack4j.model.storage.block.VolumeTransfer;
import org.openstack4j.model.storage.block.options.TransferListOptions;
import org.openstack4j.openstack.storage.block.domain.CinderVolumeTransfer.VolumeTransferList;

public class BlockVolumeTransferV3ServiceImpl extends BaseBlockStorageServices implements BlockVolumeTransferV3Service {

    @Override public List<? extends VolumeTransfer> list() { return list(TransferListOptions.create()); }
    @Override public List<? extends VolumeTransfer> listDetail() { return listDetail(TransferListOptions.create()); }

    @Override
    public List<? extends VolumeTransfer> list(TransferListOptions options) {
        requireOptions(options);
        return get(VolumeTransferList.class, uri("/volume-transfers")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public List<? extends VolumeTransfer> listDetail(TransferListOptions options) {
        requireOptions(options);
        return get(VolumeTransferList.class, uri("/volume-transfers/detail")).params(options.toQueryParams()).execute().getList();
    }

    private void requireOptions(TransferListOptions options) {
        Objects.requireNonNull(options);
        requireMicroVersion("Volume transfers API", V(55));
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Transfer list options " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
    }

    @Override
    public VolumeTransfer get(String transferId) {
        requireMicroVersion("Volume transfers API", V(55));
        return get(CinderVolumeTransfer.class, uri("/volume-transfers/%s", Objects.requireNonNull(transferId))).execute();
    }

    @Override
    public VolumeTransfer create(String volumeId, String name, Boolean noSnapshots) {
        Objects.requireNonNull(volumeId);
        requireMicroVersion("Volume transfers API", V(55));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("volume_id", volumeId);
        if (name != null) body.put("name", name);
        if (noSnapshots != null) body.put("no_snapshots", noSnapshots);
        return post(CinderVolumeTransfer.class, uri("/volume-transfers")).entity(JsonBody.of("transfer", body)).execute();
    }

    @Override
    public VolumeTransfer accept(String transferId, String authKey) {
        Objects.requireNonNull(transferId);
        Objects.requireNonNull(authKey);
        requireMicroVersion("Volume transfers API", V(55));
        return post(CinderVolumeTransfer.class, uri("/volume-transfers/%s/accept", transferId)).entity(CinderVolumeTransferAccept.create(authKey)).execute();
    }

    @Override
    public ActionResponse delete(String transferId) {
        requireMicroVersion("Volume transfers API", V(55));
        return deleteWithResponse(uri("/volume-transfers/%s", Objects.requireNonNull(transferId))).execute();
    }
}
