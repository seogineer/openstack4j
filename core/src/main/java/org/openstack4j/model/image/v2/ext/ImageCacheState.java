package org.openstack4j.model.image.v2.ext;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** The Glance image cache: cached images and images queued for caching. */
public interface ImageCacheState extends ModelEntity {
    List<? extends CachedImageEntry> getCachedImages();
    List<String> getQueuedImages();
}
