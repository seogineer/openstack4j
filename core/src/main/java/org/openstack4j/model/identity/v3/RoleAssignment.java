package org.openstack4j.model.identity.v3;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/**
 * identity v3 role assignment class
 *
 * @see <a href="http://developer.openstack.org/api-ref-identity-v3.html#roles-v3">API reference</a>
 */
public interface RoleAssignment extends ModelEntity {

    /**
     * Get role id for role assignment
     *
     * @return the id of the role
     */
    String getRoleId();

    /**
     * Get user id for role assignment
     *
     * @return the id of the user
     */
    String getUserId();

    /**
     * Get group id for role assignment
     *
     * @return the id of the group
     */
    String getGroupId();

    /**
     * Get scoped domain id for role assignment
     *
     * @return the id of the domain
     */
    String getDomainId();

    /**
     * Get scoped project id for role assignment
     *
     * @return the id of the project
     */
    String getProjectId();

    /**
     * @return the role name ({@code include_names}), or null
     */
    default String getRoleName() {
        return null;
    }

    /**
     * @return the user name ({@code include_names}), or null
     */
    default String getUserName() {
        return null;
    }

    /**
     * @return the group name ({@code include_names}), or null
     */
    default String getGroupName() {
        return null;
    }

    /**
     * @return the name of the project in the assignment scope ({@code include_names}), or null
     */
    default String getProjectName() {
        return null;
    }

    /**
     * @return the name of the domain in the assignment scope (scope.domain, not the user's domain), or null
     */
    default String getDomainName() {
        return null;
    }

    /**
     * @return the links of the role assignment
     */
    Map<String, String> getLinks();

}
