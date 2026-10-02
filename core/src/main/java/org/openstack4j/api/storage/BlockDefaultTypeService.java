package org.openstack4j.api.storage;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.DefaultVolumeType;

/** Per-project default volume types ({@code /default-types}, block storage microversion 3.62+). */
public interface BlockDefaultTypeService extends RestService {

    List<? extends DefaultVolumeType> list();

    DefaultVolumeType get(String projectId);

    /** @param volumeType the type's name or id */
    DefaultVolumeType set(String projectId, String volumeType);

    ActionResponse unset(String projectId);
}
