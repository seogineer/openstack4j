package org.openstack4j.model.identity.v3.options;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filters for {@code GET /v3/role_assignments}.
 * <p>
 * Keystone answers 400 for these combinations: project and domain scope together, domain and system scope together, project and
 * system scope together, {@code effective} with a group filter ({@code group.id}), and {@code effective} with an inherited-to
 * filter and a domain scope. {@code include_subtree} needs {@code scope.project.id}. These are not checked on the client; the
 * server message is passed through.
 * <p>
 * Keystone treats a boolean query key as true unless its value is {@code 0}, even when the value is {@code false} or empty, so the
 * boolean options here only turn the flag on and never send {@code =false}.
 */
public class RoleAssignmentListOptions {

    private final Map<String, String> params = new LinkedHashMap<>();

    public static RoleAssignmentListOptions create() {
        return new RoleAssignmentListOptions();
    }

    private RoleAssignmentListOptions put(String key, String value) {
        params.put(key, value);
        return this;
    }

    /** {@code scope.project.id} */
    public RoleAssignmentListOptions scopeProjectId(String projectId) { return put("scope.project.id", projectId); }
    /** {@code scope.domain.id} */
    public RoleAssignmentListOptions scopeDomainId(String domainId) { return put("scope.domain.id", domainId); }
    /** {@code scope.system=all} */
    public RoleAssignmentListOptions scopeSystem() { return put("scope.system", "all"); }
    /** {@code scope.OS-INHERIT:inherited_to=projects} */
    public RoleAssignmentListOptions inheritedToProjects() { return put("scope.OS-INHERIT:inherited_to", "projects"); }
    /** {@code user.id} */
    public RoleAssignmentListOptions userId(String userId) { return put("user.id", userId); }
    /** {@code group.id} */
    public RoleAssignmentListOptions groupId(String groupId) { return put("group.id", groupId); }
    /** {@code role.id} */
    public RoleAssignmentListOptions roleId(String roleId) { return put("role.id", roleId); }
    /** {@code effective=true}: expand groups and inherited assignments into user assignments. */
    public RoleAssignmentListOptions effective() { return put("effective", "true"); }
    /** {@code include_names=true}: names in addition to ids. */
    public RoleAssignmentListOptions includeNames() { return put("include_names", "true"); }
    /** {@code include_subtree=true}: also the subtree of the project; needs {@link #scopeProjectId(String)}. */
    public RoleAssignmentListOptions includeSubtree() { return put("include_subtree", "true"); }

    public Map<String, String> toQueryParams() {
        return new LinkedHashMap<>(params);
    }
}
