package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.ServiceProfile;
import org.openstack4j.model.network.options.ServiceProfileOptions;

/**
 * Neutron service profiles ({@code /v2.0/service_profiles}, flavors).
 */
public interface NeutronServiceProfileService extends RestService {

    /**
     * Lists service profiles.
     *
     * @return the result
     */
    List<? extends ServiceProfile> list();

    /**
     * Lists service profiles.
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends ServiceProfile> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    ServiceProfile get(String id);

    /**
     * Creates a service profile (admin).
     *
     * @param options the options
     * @return the result
     */
    ServiceProfile create(ServiceProfileOptions options);

    /**
     * Updates a service profile; only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    ServiceProfile update(String id, ServiceProfileOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
