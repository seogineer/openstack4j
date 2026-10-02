package org.openstack4j.api.storage;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeTransfer;
import org.openstack4j.model.storage.block.options.TransferListOptions;

/**
 * The {@code /volume-transfers} API (block storage microversion 3.55+): the same records as the legacy
 * {@code /os-volume-transfer} API plus {@code no_snapshots}, paging (3.59+) and encrypted volumes (3.70+).
 */
public interface BlockVolumeTransferV3Service extends RestService {

    List<? extends VolumeTransfer> list();

    List<? extends VolumeTransfer> list(TransferListOptions options);

    List<? extends VolumeTransfer> listDetail();

    List<? extends VolumeTransfer> listDetail(TransferListOptions options);

    VolumeTransfer get(String transferId);

    /** @param noSnapshots {@code true} to transfer the volume without its snapshots, or {@code null} */
    VolumeTransfer create(String volumeId, String name, Boolean noSnapshots);

    VolumeTransfer accept(String transferId, String authKey);

    ActionResponse delete(String transferId);
}
