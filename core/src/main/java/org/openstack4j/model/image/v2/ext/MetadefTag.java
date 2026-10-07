package org.openstack4j.model.image.v2.ext;

import org.openstack4j.model.ModelEntity;

/** A metadef tag. */
public interface MetadefTag extends ModelEntity {
    String getName();
    String getCreatedAt();
    String getUpdatedAt();
}
