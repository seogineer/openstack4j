package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.SecurityGroupDefaultStatefulness;
import org.openstack4j.model.network.options.SecurityGroupDefaultStatefulnessOptions;

/**
 * Security group default statefulness ({@code /v2.0/security-groups-default-statefulness}).
 */
public interface SecurityGroupDefaultStatefulnessService extends RestService {

    /**
     * Lists the per-project default statefulness entries.
     *
     * @return the result
     */
    List<? extends SecurityGroupDefaultStatefulness> list();

    /**
     * Lists the per-project default statefulness entries.
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends SecurityGroupDefaultStatefulness> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    SecurityGroupDefaultStatefulness get(String id);

    /**
     * Sets the default statefulness for a project.
     *
     * @param options the options
     * @return the result
     */
    SecurityGroupDefaultStatefulness create(SecurityGroupDefaultStatefulnessOptions options);

    /**
     * Changes the default statefulness.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    SecurityGroupDefaultStatefulness update(String id, SecurityGroupDefaultStatefulnessOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
