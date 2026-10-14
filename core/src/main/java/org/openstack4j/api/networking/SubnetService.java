package org.openstack4j.api.networking;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.Subnet;

/**
 * OpenStack (Neutron) Subnet based Operations
 *
 * @author Jeremy Unruh
 */
public interface SubnetService extends RestService {

    /**
     * List all the Subnet(s) which are authorized by the current Tenant
     *
     * @return the list of subnets or empty
     */
    List<? extends Subnet> list();

    /**
     * Lists subnets that match the given query parameters. A key is sent once (multi-value filters are not supported).
     *
     * @param filteringParams query parameters such as network_id, cidr, ip_version, name, tags; may be null
     * @return the list of subnets or empty
     */
    List<? extends Subnet> list(Map<String, String> filteringParams);

    /**
     * Gets a Subnet by ID
     *
     * @param subnetId the subnet identifier
     * @return the Subnet or null if not found
     */
    Subnet get(String subnetId);

    /**
     * Delete a Subnet by ID
     *
     * @param subnetId the subnet identifier to delete
     * @return the action response
     */
    ActionResponse delete(String subnetId);

    /**
     * Creates a new Subnet
     *
     * @param subnet the subnet to create
     * @return the newly created subnet
     */
    Subnet create(Subnet subnet);

    /**
     * Updates a Subnet.
     * <p>
     * Example:<br>
     * Subnet updateSN = update(existingSubnet.toBuilder().someChange(change).build());
     *
     * @param subnet the subnet to update
     * @return the new subnet info
     */
    Subnet update(Subnet subnet);

    /**
     * This method is needed if you are updating a subnet without pre-fetching the subnet prior.
     * See example at {@linkplain #update(Subnet)}
     *
     * @param subnetId the subnet identifier to update
     * @param subnet the subnet to update
     * @return the updated subnet
     */
    Subnet update(String subnetId, Subnet subnet);

}
