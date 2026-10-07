package org.openstack4j.api.image.v2;

import org.openstack4j.model.image.v2.options.ImageImportOptions;
import org.openstack4j.model.image.v2.ext.ImageLocation;
import org.openstack4j.model.image.v2.Task;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.image.v2.CachedImage;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.image.v2.ImageUpdate;
import org.openstack4j.model.image.v2.Member;

/**
 * OpenStack (Glance) Image V2 support
 *
 * @author emjburns
 */
public interface ImageService extends RestService {

    /**
     * List all available operating system images
     *
     * @return list of images
     */
    List<? extends Image> list();

    /**
     * Returns list of images filtered by parameters.
     * For filtering guidelines, see http://developer.openstack.org/api-ref/image/v2/index.html#show-images
     * To paginate, use "limit" and "marker" parameters
     *
     * @param filteringParams map (name, value) of filtering parameters
     * @return list of images fitered by filteringParams
     */
    List<? extends Image> list(Map<String, String> filteringParams);

    /**
     * List images currently in the glance image cache.
     *
     * @return list of cached images or empty list if the cache is empty or null if the cache is not enabled.
     */
    List<? extends CachedImage> listCachedImages();

    /**
     * Show details for an image by imageid.
     * The image must exist
     *
     * @return the image
     */
    Image get(String imageId);

    /**
     * Creates a catalog record for an operating system disk image.
     *
     * @return Image
     */
    Image create(Image image);

    /**
     * Update image by providing the changed image object.
     *
     * @return image
     */
    Image update(Image image);

    /**
     * Update an image by building the string of json operations
     * that represent the json transformation
     * instead of the building the whole image
     *
     * @return Image
     */
    Image update(String imageId, ImageUpdate imageUpdate);

    /**
     * Deletes an image.
     * You cannot delete images with the protected attribute set to true (boolean).
     */
    ActionResponse delete(String imageId);

    /**
     * Deactivate an image
     * If you try to download a deactivated image, you will receive a 403 (Forbidden) response code.
     * Additionally, only administrative users can view image locations for deactivated images.
     */
    ActionResponse deactivate(String imageId);

    /**
     * Reactivate an image
     */
    ActionResponse reactivate(String imageId);

    /**
     * List members of a particular image.
     * These members are projects or tenants that can see the image.
     *
     * @return List of members
     */
    List<? extends Member> listMembers(String imageId);

    /**
     * List members of a particular image.
     * These members are projects or tenants that can see the image.
     *
     * @return List of members
     */
    List<? extends Member> listMembers(String imageId, Map<String, String> filteringParams);

    /**
     * The image must exist, be private, and be owned by the author of the request.
     * Otherwise, this will fail.
     *
     * @param imageId the image to share
     * @return member
     */
    Member createMember(String imageId, String memberId);

    /**
     * Get details about a member
     *
     * @return member
     */
    Member getMember(String imageId, String memberId);

    /**
     * Change status of an image member
     * For more details see http://specs.openstack.org/openstack/glance-specs/specs/api/v2/sharing-image-api-v2.html
     *
     * @return member
     */
    Member updateMember(String imageId, String memberId, Member.MemberStatus memberStatus);

    /**
     * You must be the owner of the image to delete the member
     */
    ActionResponse deleteMember(String imageId, String memberId);

    /**
     * Add tag to image.
     * Can also be done with ImagesService#update(image)
     */
    ActionResponse updateTag(String imageId, String tag);

    /**
     * Delete tag from image.
     * Can also be done with ImagesService#update(image)
     */
    ActionResponse deleteTag(String imageId, String tag);

    /**
     * Uploads binary image data
     */
    ActionResponse upload(String imageId, Payload<?> payload, @Nullable Image image);

    /**
     * Downloads binary image data
     */
    ActionResponse download(String imageId, File filename);

    /**
     * @return the image v2 tasks service
     */
    TaskService tasks();

    /**
     * Glance discovery information: import methods, stores, usage.
     *
     * @return the ImageInfoService
     */
    org.openstack4j.api.image.v2.ext.ImageInfoService info();

    /**
     * @return the API versions of the server ({@code GET /versions} at the Glance root); use {@code supports("2.x")} before
     *         calling APIs added in later versions
     */
    org.openstack4j.model.image.v2.ext.ImageVersions versions();

    /**
     * Starts an interoperable image import (API 2.6).
     *
     * @param imageId the image (queued, or uploading after {@link #stage})
     * @param options the import method and target stores
     * @return the action response (202 Accepted; the import runs as a task)
     */
    ActionResponse importImage(String imageId, ImageImportOptions options);

    /**
     * Uploads image data to the staging area for the glance-direct import method (API 2.6).
     *
     * @param imageId the image
     * @param payload the image data, sent as application/octet-stream
     * @return the action response
     */
    ActionResponse stage(String imageId, Payload<?> payload);

    /**
     * @param imageId the image
     * @return the locations of the image data (service-to-service, API 2.17)
     */
    List<? extends ImageLocation> listLocations(String imageId);

    /**
     * Adds a location to an image that has none yet (service-to-service, API 2.17).
     *
     * @param imageId        the image
     * @param url            the location URL
     * @param validationData optional hash data ({@code os_hash_algo}, {@code os_hash_value}); null omits it
     * @return the action response
     */
    ActionResponse addLocation(String imageId, String url, Map<String, Object> validationData);

    /**
     * @param imageId the image
     * @return the tasks that worked on the image, for example imports (API 2.12)
     */
    List<? extends Task> listTasks(String imageId);

    /**
     * Deletes the image data from one store; the image stays in its other stores (multi-store, API 2.10).
     *
     * @param storeId the store
     * @param imageId the image
     * @return the action response
     */
    ActionResponse deleteFromStore(String storeId, String imageId);

    /**
     * The image cache API (API 2.14).
     *
     * @return the ImageCacheService
     */
    org.openstack4j.api.image.v2.ext.ImageCacheService cache();

    /**
     * Glance JSON schemas of images, members, tasks and metadefs.
     *
     * @return the ImageSchemaService
     */
    org.openstack4j.api.image.v2.ext.ImageSchemaService schemas();
}
