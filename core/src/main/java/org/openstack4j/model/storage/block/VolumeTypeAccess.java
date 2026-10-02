package org.openstack4j.model.storage.block;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A project allowed to use a private volume type. */
public interface VolumeTypeAccess extends ModelEntity {
    String getProjectId();
    String getVolumeTypeId();
}
