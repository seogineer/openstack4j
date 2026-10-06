package org.openstack4j.model.image.v2.ext;

import org.openstack4j.model.ModelEntity;

/** An image in the Glance cache. */
public interface CachedImageEntry extends ModelEntity {
    String getImageId();
    Integer getHits();
    Double getLastAccessed();
    Double getLastModified();
    Long getSize();
}
