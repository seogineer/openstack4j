package org.openstack4j.api.image.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.image.v2.ext.ImageStore;
import org.openstack4j.model.image.v2.ext.ImageUsage;

/**
 * Glance discovery information ({@code /v2/info}).
 */
public interface ImageInfoService extends RestService {

    /**
     * Lists the enabled image import methods (glance-direct, web-download, copy-image, glance-download).
     *
     * @return the result
     */
    List<String> importMethods();

    /**
     * Lists the stores (multi-store, API 2.8).
     *
     * @return the result
     */
    List<? extends ImageStore> stores();

    /**
     * Lists the stores with type, weight and properties (admin, API 2.15).
     *
     * @return the result
     */
    List<? extends ImageStore> storesDetail();

    /**
     * Returns the image quota usage of the caller (API 2.9).
     *
     * @return the result
     */
    Map<String, ? extends ImageUsage> usage();
}
