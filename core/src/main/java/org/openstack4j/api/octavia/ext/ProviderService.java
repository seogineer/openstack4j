package org.openstack4j.api.octavia.ext;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.octavia.ext.OctaviaProvider;
import org.openstack4j.model.octavia.ext.ProviderCapability;

/**
 * Octavia providers ({@code /v2/lbaas/providers}).
 */
public interface ProviderService extends RestService {

    /**
     * Lists the provider drivers.
     *
     * @return the result
     */
    List<? extends OctaviaProvider> list();

    /**
     * Lists the flavor capabilities a provider supports.
     *
     * @param provider the provider
     * @return the result
     */
    List<? extends ProviderCapability> flavorCapabilities(String provider);

    /**
     * Lists the availability zone capabilities a provider supports.
     *
     * @param provider the provider
     * @return the result
     */
    List<? extends ProviderCapability> availabilityZoneCapabilities(String provider);
}
