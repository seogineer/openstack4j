package org.openstack4j.api.identity.v3;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.Trust;
import org.openstack4j.model.identity.v3.options.TrustCreate;

/**
 * OS-TRUST trusts ({@code /v3/OS-TRUST/trusts}).
 */
public interface TrustService extends RestService {

    /**
     * Lists trusts; without filters only the caller's own trusts are visible to non-admins.
     *
     * @return the result
     */
    List<? extends Trust> list();

    /**
     * Lists trusts; without filters only the caller's own trusts are visible to non-admins.
     *
     * @param trustorUserId the trustor user id
     * @param trusteeUserId the trustee user id
     * @return the result
     */
    List<? extends Trust> list(String trustorUserId, String trusteeUserId);

    /**
     * @param trustId the trust id
     * @return the result
     */
    Trust get(String trustId);

    /**
     * Creates a trust: the trustor delegates the given roles on a project to the trustee.
     *
     * @param create the create
     * @return the result
     */
    Trust create(TrustCreate create);

    /**
     * @param trustId the trust id
     * @return the action response
     */
    ActionResponse delete(String trustId);

    /**
     * Lists the roles delegated by the trust.
     *
     * @param trustId the trust id
     * @return the result
     */
    List<? extends Role> roles(String trustId);

    /**
     * @param trustId the trust id
     * @param roleId the role id
     * @return the result
     */
    Role getRole(String trustId, String roleId);

    /**
     * Checks that the trust delegates the role (HEAD).
     *
     * @param trustId the trust id
     * @param roleId the role id
     * @return the action response
     */
    ActionResponse checkRole(String trustId, String roleId);
}
