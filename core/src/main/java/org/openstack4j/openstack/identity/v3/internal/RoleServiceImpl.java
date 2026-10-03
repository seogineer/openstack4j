package org.openstack4j.openstack.identity.v3.internal;

import org.openstack4j.openstack.identity.v3.domain.KeystoneRoleInference.RoleInferenceResponse;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRoleInference.RoleInferences;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRoleInference;
import org.openstack4j.model.identity.v3.RoleInference;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.RoleService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.model.identity.v3.RoleAssignment;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole.Roles;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRoleAssignment.RoleAssignments;

import static org.openstack4j.core.transport.ClientConstants.PATH_ROLES;

/**
 * Identity Role based Operations Implementation
 */
public class RoleServiceImpl extends BaseIdentityServices implements RoleService {

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse grantProjectUserRole(String projectId, String userId, String roleId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(projectId);
        Objects.requireNonNull(roleId);
        return put(ActionResponse.class, uri("projects/%s/users/%s/roles/%s", projectId, userId, roleId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse revokeProjectUserRole(String projectId, String userId, String roleId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(projectId);
        Objects.requireNonNull(roleId);
        return deleteWithResponse(uri("projects/%s/users/%s/roles/%s", projectId, userId, roleId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse checkProjectUserRole(String projectId, String userId, String roleId) {
        Objects.requireNonNull(projectId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(roleId);
        return head(ActionResponse.class, uri("/projects/%s/users/%s/roles/%s", projectId, userId, roleId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse grantDomainUserRole(String domainId, String userId, String roleId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(domainId);
        Objects.requireNonNull(roleId);
        return put(ActionResponse.class, uri("domains/%s/users/%s/roles/%s", domainId, userId, roleId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse revokeDomainUserRole(String domainId, String userId, String roleId) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(domainId);
        Objects.requireNonNull(roleId);
        return deleteWithResponse(uri("domains/%s/users/%s/roles/%s", domainId, userId, roleId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse checkDomainUserRole(String domainId, String userId, String roleId) {
        Objects.requireNonNull(domainId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(roleId);
        return head(ActionResponse.class, uri("/domains/%s/users/%s/roles/%s", domainId, userId, roleId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Role> list() {
        return get(Roles.class, uri("/roles")).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Role> getByName(String name) {
        Objects.requireNonNull(name);
        return get(Roles.class, "/roles").param("name", name).execute().getList();

    }

    @Override
    public Role create(Role role) {
        Objects.requireNonNull(role);
        return post(KeystoneRole.class, uri(PATH_ROLES)).entity(role).execute();
    }

    @Override
    public Role create(String name) {
        Objects.requireNonNull(name);
        return create(KeystoneRole.builder().name(name).build());
    }

    @Override
    public Role update(Role role) {
        Objects.requireNonNull(role);
        return patch(KeystoneRole.class, PATH_ROLES, "/", role.getId()).entity(role).execute();
    }

    @Override
    public ActionResponse delete(String roleId) {
        Objects.requireNonNull(roleId);
        return deleteWithResponse(PATH_ROLES, "/", roleId).execute();
    }

    @Override
    public Role get(String roleId) {
        Objects.requireNonNull(roleId);
        return get(KeystoneRole.class, PATH_ROLES, "/", roleId).execute();
    }

    @Override
    public List<? extends RoleAssignment> listRoleAssignments(String projectId) {
        Objects.requireNonNull(projectId);
        return get(RoleAssignments.class, "role_assignments")
                .param("scope.project.id", projectId)
                .param("effective", "true")
                .execute().getList();
    }

    @Override
    public ActionResponse grantProjectGroupRole(String projectId, String groupId, String roleId) {
        Objects.requireNonNull(projectId);
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(roleId);
        return put(ActionResponse.class, uri("projects/%s/groups/%s/roles/%s", projectId, groupId, roleId)).execute();
    }

    @Override
    public ActionResponse revokeProjectGroupRole(String projectId, String groupId, String roleId) {
        Objects.requireNonNull(projectId);
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(roleId);
        return deleteWithResponse(uri("projects/%s/groups/%s/roles/%s", projectId, groupId, roleId)).execute();
    }

    @Override
    public ActionResponse checkProjectGroupRole(String projectId, String groupId, String roleId) {
        Objects.requireNonNull(projectId);
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(roleId);
        return head(ActionResponse.class, uri("/projects/%s/groups/%s/roles/%s", projectId, groupId, roleId)).execute();
    }

    @Override
    public ActionResponse grantDomainGroupRole(String domainId, String groupId, String roleId) {
        Objects.requireNonNull(domainId);
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(roleId);
        return put(ActionResponse.class, uri("domains/%s/groups/%s/roles/%s", domainId, groupId, roleId)).execute();
    }

    @Override
    public ActionResponse revokeDomainGroupRole(String domainId, String groupId, String roleId) {
        Objects.requireNonNull(domainId);
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(roleId);
        return deleteWithResponse(uri("domains/%s/groups/%s/roles/%s", domainId, groupId, roleId)).execute();
    }

    @Override
    public ActionResponse checkDomainGroupRole(String domainId, String groupId, String roleId) {
        Objects.requireNonNull(domainId);
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(roleId);
        return head(ActionResponse.class, uri("/domains/%s/groups/%s/roles/%s", domainId, groupId, roleId)).execute();
    }


    private static String inherited(String targetType, String targetId, String actorType, String actorId, String roleId) {
        return String.format("/OS-INHERIT/%s/%s/%s/%s/roles/%s/inherited_to_projects", targetType, Objects.requireNonNull(targetId),
                actorType, Objects.requireNonNull(actorId), Objects.requireNonNull(roleId));
    }

    @Override public ActionResponse grantInheritedRoleToUserOnDomain(String d, String u, String r) { return put(ActionResponse.class, inherited("domains", d, "users", u, r)).execute(); }
    @Override public ActionResponse revokeInheritedRoleFromUserOnDomain(String d, String u, String r) { return deleteWithResponse(inherited("domains", d, "users", u, r)).execute(); }
    @Override public ActionResponse checkInheritedRoleOfUserOnDomain(String d, String u, String r) { return head(ActionResponse.class, inherited("domains", d, "users", u, r)).execute(); }
    @Override public List<? extends Role> listInheritedRolesOfUserOnDomain(String d, String u) { return get(Roles.class, uri("/OS-INHERIT/domains/%s/users/%s/roles/inherited_to_projects", Objects.requireNonNull(d), Objects.requireNonNull(u))).execute().getList(); }
    @Override public ActionResponse grantInheritedRoleToGroupOnDomain(String d, String g, String r) { return put(ActionResponse.class, inherited("domains", d, "groups", g, r)).execute(); }
    @Override public ActionResponse revokeInheritedRoleFromGroupOnDomain(String d, String g, String r) { return deleteWithResponse(inherited("domains", d, "groups", g, r)).execute(); }
    @Override public ActionResponse checkInheritedRoleOfGroupOnDomain(String d, String g, String r) { return head(ActionResponse.class, inherited("domains", d, "groups", g, r)).execute(); }
    @Override public List<? extends Role> listInheritedRolesOfGroupOnDomain(String d, String g) { return get(Roles.class, uri("/OS-INHERIT/domains/%s/groups/%s/roles/inherited_to_projects", Objects.requireNonNull(d), Objects.requireNonNull(g))).execute().getList(); }
    @Override public ActionResponse grantInheritedRoleToUserOnProject(String p, String u, String r) { return put(ActionResponse.class, inherited("projects", p, "users", u, r)).execute(); }
    @Override public ActionResponse revokeInheritedRoleFromUserOnProject(String p, String u, String r) { return deleteWithResponse(inherited("projects", p, "users", u, r)).execute(); }
    @Override public ActionResponse checkInheritedRoleOfUserOnProject(String p, String u, String r) { return head(ActionResponse.class, inherited("projects", p, "users", u, r)).execute(); }
    @Override public ActionResponse grantInheritedRoleToGroupOnProject(String p, String g, String r) { return put(ActionResponse.class, inherited("projects", p, "groups", g, r)).execute(); }
    @Override public ActionResponse revokeInheritedRoleFromGroupOnProject(String p, String g, String r) { return deleteWithResponse(inherited("projects", p, "groups", g, r)).execute(); }
    @Override public ActionResponse checkInheritedRoleOfGroupOnProject(String p, String g, String r) { return head(ActionResponse.class, inherited("projects", p, "groups", g, r)).execute(); }

    private static String implies(String priorRoleId, String impliedRoleId) {
        return "/roles/" + Objects.requireNonNull(priorRoleId) + "/implies/" + Objects.requireNonNull(impliedRoleId);
    }

    @Override
    public List<? extends RoleInference> listImpliedRoles(String priorRoleId) {
        KeystoneRoleInference one = inference(get(RoleInferenceResponse.class, uri("/roles/%s/implies", Objects.requireNonNull(priorRoleId))).execute());
        return one == null ? Collections.emptyList() : Collections.singletonList(one);
    }

    private static KeystoneRoleInference inference(RoleInferenceResponse response) {
        return response == null ? null : response.get();
    }

    @Override public RoleInference getImpliedRole(String prior, String implied) { return inference(get(RoleInferenceResponse.class, implies(prior, implied)).execute()); }
    @Override public ActionResponse checkImpliedRole(String prior, String implied) { return head(ActionResponse.class, implies(prior, implied)).execute(); }
    @Override public RoleInference createImpliedRole(String prior, String implied) { return inference(put(RoleInferenceResponse.class, implies(prior, implied)).execute()); }
    @Override public ActionResponse deleteImpliedRole(String prior, String implied) { return deleteWithResponse(implies(prior, implied)).execute(); }
    @Override public List<? extends RoleInference> listRoleInferences() { return get(RoleInferences.class, uri("/role_inferences")).execute().getList(); }
}
