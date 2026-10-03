package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.AddressScope;
import org.openstack4j.model.network.options.AddressScopeOptions;

/**
 * Address scopes ({@code /v2.0/address-scopes}, address-scope).
 */
public interface AddressScopeService extends RestService {

    /**
     * Lists address scopes, optionally filtered.
     *
     * @return the result
     */
    List<? extends AddressScope> list();

    /**
     * Lists address scopes, optionally filtered.
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends AddressScope> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    AddressScope get(String id);

    /**
     * Creates an address scope.
     *
     * @param options the options
     * @return the result
     */
    AddressScope create(AddressScopeOptions options);

    /**
     * Updates an address scope; only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    AddressScope update(String id, AddressScopeOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
