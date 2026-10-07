package org.openstack4j.api.identity.v3;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.ServiceProvider;

/**
 * OS-FEDERATION Keystone-to-Keystone service providers ({@code /v3/OS-FEDERATION/service_providers}).
 */
public interface ServiceProviderService extends RestService {

    /**
     * @return the result
     */
    List<? extends ServiceProvider> list();

    /**
     * @param id the id
     * @return the result
     */
    ServiceProvider get(String id);

    /**
     * Creates a service provider (PUT); attribute keys: auth_url, sp_url, description, enabled, relay_state_prefix.
     *
     * @param id the id
     * @param attributes the attributes
     * @return the result
     */
    ServiceProvider create(String id, Map<String, Object> attributes);

    /**
     * Updates a service provider (PATCH) with the given attributes.
     *
     * @param id the id
     * @param attributes the attributes
     * @return the result
     */
    ServiceProvider update(String id, Map<String, Object> attributes);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
