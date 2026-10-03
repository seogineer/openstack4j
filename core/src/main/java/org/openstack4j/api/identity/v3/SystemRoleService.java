package org.openstack4j.api.identity.v3;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Role;

/**
 * System role assignments ({@code /v3/system/{users,groups}/{id}/roles}): roles over the whole deployment
 * rather than one project or domain.
 */
public interface SystemRoleService extends RestService {

    /**
     * @param userId the user
     * @return the user's system roles
     */
    List<? extends Role> listUserRoles(String userId);

    /**
     * @param userId the user
     * @param roleId the role
     * @return the role when the user has it on the system
     */
    Role getUserRole(String userId, String roleId);

    /**
     * @param userId the user
     * @param roleId the role
     * @return success when the user has the role on the system
     */
    ActionResponse checkUserRole(String userId, String roleId);

    /**
     * @param userId the user
     * @param roleId the role
     * @return the action response
     */
    ActionResponse grantUserRole(String userId, String roleId);

    /**
     * @param userId the user
     * @param roleId the role
     * @return the action response
     */
    ActionResponse revokeUserRole(String userId, String roleId);

    /**
     * @param groupId the group
     * @return the group's system roles
     */
    List<? extends Role> listGroupRoles(String groupId);

    /**
     * @param groupId the group
     * @param roleId the role
     * @return the role when the group has it on the system
     */
    Role getGroupRole(String groupId, String roleId);

    /**
     * @param groupId the group
     * @param roleId the role
     * @return success when the group has the role on the system
     */
    ActionResponse checkGroupRole(String groupId, String roleId);

    /**
     * @param groupId the group
     * @param roleId the role
     * @return the action response
     */
    ActionResponse grantGroupRole(String groupId, String roleId);

    /**
     * @param groupId the group
     * @param roleId the role
     * @return the action response
     */
    ActionResponse revokeGroupRole(String groupId, String roleId);
}
