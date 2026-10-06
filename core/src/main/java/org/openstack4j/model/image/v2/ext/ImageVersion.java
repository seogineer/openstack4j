package org.openstack4j.model.image.v2.ext;

import org.openstack4j.model.ModelEntity;

/** A Glance API version. */
public interface ImageVersion extends ModelEntity {
    String getId();
    String getStatus();
}
