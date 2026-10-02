package org.openstack4j.api.storage;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.storage.block.ManageableVolume;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.options.ManageableListOptions;
import org.openstack4j.model.storage.block.options.VolumeManageRequest;

/** Manageable volumes ({@code /manageable_volumes}, block storage microversion 3.8+; admin). */
public interface BlockManageableVolumeService extends RestService {

    List<? extends ManageableVolume> list(ManageableListOptions options);

    List<? extends ManageableVolume> listDetail(ManageableListOptions options);

    /** Brings a backend volume under Cinder's control; the result is in status {@code creating} until managed. */
    Volume manage(VolumeManageRequest request);
}
