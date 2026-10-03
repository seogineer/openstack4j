package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.AddressGroup;
import org.openstack4j.model.network.options.AddressGroupOptions;

/**
 * Address groups ({@code /v2.0/address-groups}, address-group): CIDR sets security group rules can reference.
 */
public interface AddressGroupService extends RestService {

    /**
     * Lists address groups, optionally filtered.
     *
     * @return the result
     */
    List<? extends AddressGroup> list();

    /**
     * Lists address groups, optionally filtered.
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends AddressGroup> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    AddressGroup get(String id);

    /**
     * Creates an address group.
     *
     * @param options the options
     * @return the result
     */
    AddressGroup create(AddressGroupOptions options);

    /**
     * Updates the name or description of an address group.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    AddressGroup update(String id, AddressGroupOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);

    /**
     * Adds CIDRs to the group.
     *
     * @param id the id
     * @param addresses the addresses
     * @return the result
     */
    AddressGroup addAddresses(String id, List<String> addresses);

    /**
     * Removes CIDRs from the group.
     *
     * @param id the id
     * @param addresses the addresses
     * @return the result
     */
    AddressGroup removeAddresses(String id, List<String> addresses);
}
