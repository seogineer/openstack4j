package org.openstack4j.api.image.v2.ext;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.v2.ext.ImageCacheState;

/**
 * The Glance image cache API ({@code /v2/cache}, API 2.14; admin; needs the cache middleware).
 */
public interface ImageCacheService extends RestService {

    /**
     * Lists the cached and queued images.
     *
     * @return the result
     */
    ImageCacheState list();

    /**
     * Queues an image for caching.
     *
     * @param imageId the image id
     * @return the action response
     */
    ActionResponse queue(String imageId);

    /**
     * Removes an image from the cache.
     *
     * @param imageId the image id
     * @return the action response
     */
    ActionResponse delete(String imageId);

    /**
     * Clears the cache and the queue; with a target (cache or queue) only that one.
     *
     * @return the action response
     */
    ActionResponse clear();

    /**
     * Clears the cache and the queue; with a target (cache or queue) only that one.
     *
     * @param target the target
     * @return the action response
     */
    ActionResponse clear(String target);

    /**
     * Removes invalid and stalled cache entries.
     *
     * @return the action response
     */
    ActionResponse clean();

    /**
     * Shrinks the cache to its configured maximum size.
     *
     * @return the action response
     */
    ActionResponse prune();
}
