package org.openstack4j.openstack.image.v2.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceVersions;
import org.openstack4j.openstack.internal.BaseOpenStackService;

/** Fetches {@code GET /versions} from the Glance root (outside {@code /v2}). */
public final class ImageVersionDiscovery extends BaseOpenStackService {

    public ImageVersionDiscovery() {
        super(ServiceType.IMAGE, ImageVersionDiscovery::rootUrl);
    }

    /** The endpoint without a trailing {@code /v2}, {@code /v2.x} or slash. */
    public static String rootUrl(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.replaceAll("/v2(\\.\\d+)?$", "");
    }

    /** Glance answers 300 Multiple Choices with the versions document as body. */
    public GlanceVersions fetch() {
        return request(HttpMethod.GET, GlanceVersions.class, "/versions").execute();
    }
}
