package org.openstack4j.openstack.identity.v3.internal;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.SystemRoleService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Role;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRole.Roles;

public class SystemRoleServiceImpl extends BaseIdentityServices implements SystemRoleService {

    private static String path(String actor, String actorId, String roleId) {
        String base = "/system/" + actor + "/" + Objects.requireNonNull(actorId) + "/roles";
        return roleId == null ? base : base + "/" + roleId;
    }

    @Override public List<? extends Role> listUserRoles(String userId) { return get(Roles.class, path("users", userId, null)).execute().getList(); }
    @Override public Role getUserRole(String userId, String roleId) { return get(KeystoneRole.class, path("users", userId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse checkUserRole(String userId, String roleId) { return head(ActionResponse.class, path("users", userId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse grantUserRole(String userId, String roleId) { return put(ActionResponse.class, path("users", userId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse revokeUserRole(String userId, String roleId) { return deleteWithResponse(path("users", userId, Objects.requireNonNull(roleId))).execute(); }
    @Override public List<? extends Role> listGroupRoles(String groupId) { return get(Roles.class, path("groups", groupId, null)).execute().getList(); }
    @Override public Role getGroupRole(String groupId, String roleId) { return get(KeystoneRole.class, path("groups", groupId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse checkGroupRole(String groupId, String roleId) { return head(ActionResponse.class, path("groups", groupId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse grantGroupRole(String groupId, String roleId) { return put(ActionResponse.class, path("groups", groupId, Objects.requireNonNull(roleId))).execute(); }
    @Override public ActionResponse revokeGroupRole(String groupId, String roleId) { return deleteWithResponse(path("groups", groupId, Objects.requireNonNull(roleId))).execute(); }
}
