package org.openstack4j.model.image.v2.options;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Body of {@code POST /v2/images/{id}/import} (interoperable image import, API 2.6+). */
public class ImageImportOptions extends ImageAttributes<ImageImportOptions> {

    private final Map<String, Object> method = new LinkedHashMap<>();

    private ImageImportOptions(String name) {
        method.put("name", name);
        attribute("method", method);
    }

    /** Imports data previously sent with {@code stage()}. */
    public static ImageImportOptions glanceDirect() { return new ImageImportOptions("glance-direct"); }

    /** Glance downloads the image from a URI. */
    public static ImageImportOptions webDownload(String uri) {
        ImageImportOptions options = new ImageImportOptions("web-download");
        options.method.put("uri", Objects.requireNonNull(uri));
        return options;
    }

    /** Copies an existing image into more stores. */
    public static ImageImportOptions copyImage(List<String> stores) {
        return new ImageImportOptions("copy-image").stores(Objects.requireNonNull(stores));
    }

    /** Downloads the image from the Glance of another region. */
    public static ImageImportOptions glanceDownload(String regionName, String imageId) {
        ImageImportOptions options = new ImageImportOptions("glance-download");
        options.method.put("glance_region", Objects.requireNonNull(regionName));
        options.method.put("glance_image_id", Objects.requireNonNull(imageId));
        return options;
    }

    @Override
    protected ImageImportOptions self() {
        return this;
    }

    /** For glance-download: the interface of the remote Glance endpoint (public, internal, admin). */
    public ImageImportOptions serviceInterface(String serviceInterface) {
        if (serviceInterface != null)
            method.put("glance_service_interface", serviceInterface);
        return this;
    }

    public ImageImportOptions stores(List<String> stores) { return put("stores", stores); }
    public ImageImportOptions allStores(boolean allStores) { return put("all_stores", allStores); }
    public ImageImportOptions allStoresMustSucceed(boolean mustSucceed) { return put("all_stores_must_succeed", mustSucceed); }
}
