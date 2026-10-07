package org.openstack4j.model.image.v2.ext;

import org.openstack4j.model.ModelEntity;

/** A resource type metadefs can apply to (for example OS::Glance::Image). */
public interface MetadefResourceType extends ModelEntity {
    String getName();
    String getCreatedAt();
    String getUpdatedAt();
}
