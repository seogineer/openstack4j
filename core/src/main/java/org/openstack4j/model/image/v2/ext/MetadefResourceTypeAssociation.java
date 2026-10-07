package org.openstack4j.model.image.v2.ext;

import org.openstack4j.model.ModelEntity;

/** An association of a namespace with a resource type. */
public interface MetadefResourceTypeAssociation extends ModelEntity {
    String getName();
    String getPrefix();
    String getPropertiesTarget();
    String getCreatedAt();
    String getUpdatedAt();
}
