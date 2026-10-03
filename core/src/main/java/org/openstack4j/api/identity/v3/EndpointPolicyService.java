package org.openstack4j.api.identity.v3;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.Policy;

/**
 * OS-ENDPOINT-POLICY: associations between policies and endpoints, services and regions.
 */
public interface EndpointPolicyService extends RestService {

    /**
     * Associates the policy with an endpoint.
     *
     * @param p the p
     * @param e the e
     * @return the action response
     */
    ActionResponse associateWithEndpoint(String p, String e);

    /**
     * Verifies the association (GET).
     *
     * @param p the p
     * @param e the e
     * @return the action response
     */
    ActionResponse checkEndpointAssociation(String p, String e);

    /**
     * @param p the p
     * @param e the e
     * @return the action response
     */
    ActionResponse disassociateFromEndpoint(String p, String e);

    /**
     * Associates the policy with every endpoint of a service.
     *
     * @param p the p
     * @param s the s
     * @return the action response
     */
    ActionResponse associateWithService(String p, String s);

    /**
     * Verifies the association (GET).
     *
     * @param p the p
     * @param s the s
     * @return the action response
     */
    ActionResponse checkServiceAssociation(String p, String s);

    /**
     * @param p the p
     * @param s the s
     * @return the action response
     */
    ActionResponse disassociateFromService(String p, String s);

    /**
     * Associates the policy with the endpoints of a service in a region.
     *
     * @param p the p
     * @param s the s
     * @param r the r
     * @return the action response
     */
    ActionResponse associateWithServiceInRegion(String p, String s, String r);

    /**
     * Verifies the association (GET).
     *
     * @param p the p
     * @param s the s
     * @param r the r
     * @return the action response
     */
    ActionResponse checkServiceInRegionAssociation(String p, String s, String r);

    /**
     * @param p the p
     * @param s the s
     * @param r the r
     * @return the action response
     */
    ActionResponse disassociateFromServiceInRegion(String p, String s, String r);

    /**
     * Lists the endpoints the policy applies to.
     *
     * @param p the p
     * @return the result
     */
    List<? extends Endpoint> endpointsForPolicy(String p);

    /**
     * Returns the policy that applies to the endpoint.
     *
     * @param e the e
     * @return the result
     */
    Policy policyForEndpoint(String e);

    /**
     * Checks the policy association resource (HEAD /policies/{id}/OS-ENDPOINT-POLICY/policy).
     *
     * @param p the p
     * @return the action response
     */
    ActionResponse checkPolicyAssociations(String p);
}
