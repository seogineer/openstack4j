package org.openstack4j.api.identity.v3;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.Extension;

/**
 * Identity v3 Service Operations API
 */
public interface IdentityService extends RestService {

    /**
     * Credential Service API
     *
     * @return the credential service
     */
    CredentialService credentials();

    /**
     * Domain Service API
     *
     * @return the domain service
     */
    DomainService domains();

    /**
     * Projects Service API
     *
     * @return the project service
     */
    ProjectService projects();

    /**
     * Users Service API
     *
     * @return the user service
     */
    UserService users();

    /**
     * Role Service API
     *
     * @return the role service
     */
    RoleService roles();

    /**
     * Group Service API
     *
     * @return the group service
     */
    GroupService groups();

    /**
     * Token Service API
     *
     * @return the token service
     */
    TokenService tokens();

    /**
     * Policy Service API
     *
     * @return the policy service
     */
    PolicyService policies();

    /**
     * ServiceEndpoint Service API
     *
     * @return the service and endpoint service
     */
    ServiceEndpointService serviceEndpoints();

    /**
     * Region Service API
     *
     * @return the region service
     */
    RegionService regions();

    /**
     * List extensions currently available on the OpenStack instance
     *
     * @return List of extensions
     * @deprecated https://docs.openstack.org/api-ref/compute/?expanded=#extensions-extensions-deprecated
     */
    @Deprecated
    List<? extends Extension> listExtensions();


    /**
     * Application credentials of users: list, get, create (with roles, access rules, expiry) and delete.
     *
     * @return the ApplicationCredentialService
     */
    ApplicationCredentialService applicationCredentials();

    /**
     * System role assignments of users and groups.
     *
     * @return the SystemRoleService
     */
    SystemRoleService systemRoles();

    /**
     * OS-TRUST trusts: delegate roles on a project from a trustor to a trustee.
     *
     * @return the TrustService
     */
    TrustService trusts();

    /**
     * OS-EP-FILTER endpoint groups and project-endpoint associations.
     *
     * @return the EndpointFilterService
     */
    EndpointFilterService endpointFilter();

    /**
     * OS-ENDPOINT-POLICY policy associations.
     *
     * @return the EndpointPolicyService
     */
    EndpointPolicyService endpointPolicies();

    /**
     * Unified limits: registered limits.
     *
     * @return the RegisteredLimitService
     */
    RegisteredLimitService registeredLimits();

    /**
     * Unified limits: project/domain limits and the enforcement model.
     *
     * @return the LimitService
     */
    LimitService limits();
}
