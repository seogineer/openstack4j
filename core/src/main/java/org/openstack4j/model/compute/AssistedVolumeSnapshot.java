package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/** Result of an assisted volume snapshot (used by Cinder volume drivers; admin only). */
public interface AssistedVolumeSnapshot extends ModelEntity {
    String getId();
    String getVolumeId();
}
