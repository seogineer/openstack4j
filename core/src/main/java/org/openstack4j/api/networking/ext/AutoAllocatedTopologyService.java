package org.openstack4j.api.networking.ext;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.AutoAllocatedTopology;

/**
 * Auto-allocated topology (get-me-a-network) ({@code /v2.0/auto-allocated-topology/{project_id}}).
 */
public interface AutoAllocatedTopologyService extends RestService {

    /**
     * Returns (and creates on first use) the auto-allocated topology of a project.
     *
     * @param projectId the project id
     * @return the result
     */
    AutoAllocatedTopology get(String projectId);

    /**
     * Checks whether the topology can be allocated ({@code ?fields=dry-run}); a missing default external network or subnet pool is reported as an error.
     *
     * @param projectId the project id
     * @return the result
     */
    AutoAllocatedTopology validate(String projectId);

    /**
     * Deletes the auto-allocated topology of a project.
     *
     * @param projectId the project id
     * @return the action response
     */
    ActionResponse delete(String projectId);
}
