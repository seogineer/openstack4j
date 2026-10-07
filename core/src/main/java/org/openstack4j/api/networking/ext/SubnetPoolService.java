package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.SubnetPool;
import org.openstack4j.model.network.options.SubnetPoolOptions;

/**
 * Subnet pools ({@code /v2.0/subnetpools}, subnet_allocation, subnetpool-prefix-ops).
 */
public interface SubnetPoolService extends RestService {

    /**
     * Lists subnet pools.
     *
     * @return the result
     */
    List<? extends SubnetPool> list();

    /**
     * Lists subnet pools, optionally filtered (for example ip_version, shared, address_scope_id).
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends SubnetPool> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    SubnetPool get(String id);

    /**
     * Creates a subnet pool.
     *
     * @param options the options
     * @return the result
     */
    SubnetPool create(SubnetPoolOptions options);

    /**
     * Updates a subnet pool; only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    SubnetPool update(String id, SubnetPoolOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);

    /**
     * Adds prefixes; returns the pool prefixes after Neutron merges adjacent ones.
     *
     * @param id the id
     * @param prefixes the prefixes
     * @return the result
     */
    List<String> addPrefixes(String id, List<String> prefixes);

    /**
     * Removes prefixes that no subnet uses; returns the remaining prefixes.
     *
     * @param id the id
     * @param prefixes the prefixes
     * @return the result
     */
    List<String> removePrefixes(String id, List<String> prefixes);

    /**
     * Moves the subnets of a network into the pool (subnet_onboard).
     *
     * @param id the id
     * @param networkId the network id
     * @return the result
     */
    List<Map<String, Object>> onboardNetworkSubnets(String id, String networkId);
}
