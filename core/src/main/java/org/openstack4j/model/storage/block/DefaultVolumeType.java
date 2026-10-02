package org.openstack4j.model.storage.block;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** The default volume type of a project (3.62+). */
public interface DefaultVolumeType extends ModelEntity {
    String getProjectId();
    String getVolumeTypeId();
}
