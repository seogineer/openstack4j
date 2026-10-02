package org.openstack4j.api.placement;

import org.openstack4j.api.placement.ext.ResourceProviderService;
import org.openstack4j.common.RestService;

/**
 * Placement Operations API
 *
 * @author Jyothi Saroja
 */
public interface PlacementService extends RestService {

    /**
     * Resource provider Service API
     *
     * @return the resource provider service
     */
    ResourceProviderService resourceProviders();

    /**
     * @return the Placement microversion range of the server and the version this session uses
     */
    org.openstack4j.api.placement.v1.VersionService versions();

    /**
     * Pins the Placement microversion used by the {@code v1} services for this session, or returns to automatic
     * negotiation when {@code version} is {@code null}.
     *
     * @throws org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException if the version is outside
     *         the range supported by both the library and the server
     */
    void useMicroVersion(String version);

    /**
     * Placement resource providers with the full v1 API; see also the older {@link #resourceProviders()}.
     */
    org.openstack4j.api.placement.v1.ResourceProviderService providers();

}
