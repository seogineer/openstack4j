package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.util.ToStringHelper;
import org.openstack4j.model.identity.v3.RoleAssignment;
import org.openstack4j.openstack.common.ListResult;

/**
 * v3 role assignment implementation
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneRoleAssignment implements RoleAssignment {

    private static final long serialVersionUID = 1L;

    private String roleId;

    private String userId;

    private String groupId;

    private String domainId;

    private String projectId;

    private String roleName;

    private String userName;

    private String groupName;

    private String projectName;

    private String domainName;

    private Map<String, String> links;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRoleId() {
        return roleId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserId() {
        return userId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGroupId() {
        return groupId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainId() {
        return domainId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProjectId() {
        return projectId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRoleName() {
        return roleName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserName() {
        return userName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGroupName() {
        return groupName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProjectName() {
        return projectName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        return domainName;
    }

    @JsonProperty("user")
    public void setUser(Map<String, Object> user) {
        userId = (String) user.get("id");
        userName = (String) user.get("name");
    }

    @JsonProperty("role")
    public void setRole(Map<String, Object> role) {
        roleId = (String) role.get("id");
        roleName = (String) role.get("name");
    }

    @JsonProperty("group")
    public void setGroup(Map<String, Object> group) {
        groupId = (String) group.get("id");
        groupName = (String) group.get("name");
    }

    @JsonProperty("scope")
    public void setScope(Map<String, Object> group) {
        Map<String, Object> projectScope = (Map<String, Object>) group.get("project");
        if (projectScope != null) {
            projectId = (String) projectScope.get("id");
            projectName = (String) projectScope.get("name");
        }
        Map<String, Object> domainScope = (Map<String, Object>) group.get("domain");
        if (domainScope != null) {
            domainId = (String) domainScope.get("id");
            domainName = (String) domainScope.get("name");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getLinks() {
        return links;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
                .add("roleId", roleId)
                .add("roleName", roleName)
                .add("groupId", groupId)
                .add("groupName", groupName)
                .add("userId", userId)
                .add("userName", userName)
                .add("projectId", projectId)
                .add("projectName", projectName)
                .add("domainId", domainId)
                .add("domainName", domainName)
                .add("links", links)
                .toString();
    }

    public static class RoleAssignments extends ListResult<KeystoneRoleAssignment> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("role_assignments")
        protected List<KeystoneRoleAssignment> list;

        @Override
        public List<KeystoneRoleAssignment> value() {
            return list;
        }
    }
}
