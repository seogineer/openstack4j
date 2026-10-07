package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.ServiceFlavor;
import org.openstack4j.model.network.options.ServiceFlavorOptions;

/**
 * Neutron service flavors ({@code /v2.0/flavors}, flavors), for example router flavors; not compute flavors.
 */
public interface ServiceFlavorService extends RestService {

    /**
     * Lists service flavors.
     *
     * @return the result
     */
    List<? extends ServiceFlavor> list();

    /**
     * Lists service flavors, optionally filtered (service_type).
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends ServiceFlavor> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    ServiceFlavor get(String id);

    /**
     * Creates a service flavor (admin).
     *
     * @param options the options
     * @return the result
     */
    ServiceFlavor create(ServiceFlavorOptions options);

    /**
     * Updates a service flavor; only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    ServiceFlavor update(String id, ServiceFlavorOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);

    /**
     * Associates a service profile with the flavor.
     *
     * @param flavorId the flavor id
     * @param profileId the profile id
     * @return the action response
     */
    ActionResponse associateProfile(String flavorId, String profileId);

    /**
     * Removes a service profile from the flavor.
     *
     * @param flavorId the flavor id
     * @param profileId the profile id
     * @return the action response
     */
    ActionResponse disassociateProfile(String flavorId, String profileId);
}
