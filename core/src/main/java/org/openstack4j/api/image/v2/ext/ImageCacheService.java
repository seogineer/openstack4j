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
     * Clears both the cache and the queue.
     *
     * @return the action response
     */
    ActionResponse clear();

    /**
     * Clears only the cache or only the queue ({@code x-image-cache-clear-target}); a null target clears both.
     *
     * @param target the target
     * @return the action response
     */
    ActionResponse clear(String target);

    /**
     * Removes invalid and stalled cache entries (API 2.18).
     *
     * @return the action response
     */
    ActionResponse clean();

    /**
     * Shrinks the cache to its configured maximum size (API 2.18).
     *
     * @return the action response
     */
    ActionResponse prune();
}
