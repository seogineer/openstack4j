package org.openstack4j.openstack.manila.internal.ext;

import static org.openstack4j.openstack.manila.internal.ManilaMicroVersions.V;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.manila.ext.ShareServerExtService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareServerInfo;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareServerInfo;

public class ShareServerExtServiceImpl extends BaseManilaExtService implements ShareServerExtService {

    private static final MicroVersion MANAGE = V(49);
    private static final MicroVersion MIGRATION = V(57);
    /** Share server migration is still experimental at the latest microversion. */
    private static final MicroVersion NEVER_STABLE = V(1000);
    private static final MicroVersion SECURITY = V(63);

    private static String server(String shareServerId) {
        return "/share-servers/" + id(shareServerId);
    }

    private static String network(String shareNetworkId) {
        return "/share-networks/" + id(shareNetworkId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> details(String shareServerId) {
        Map<String, Object> body = showStrict(V(7), Map.class, server(shareServerId) + "/details");
        Object details = body == null ? null : body.get("details");
        return details instanceof Map ? (Map<String, Object>) details : new LinkedHashMap<>();
    }

    @Override
    public ShareServerInfo manage(Map<String, ?> shareServer) {
        return at(MANAGE, post(ManilaShareServerInfo.class, "/share-servers/manage"), "/share-servers/manage")
                .entity(JsonBody.of("share_server", Objects.requireNonNull(shareServer, "shareServer"))).execute(propagate404());
    }

    @Override
    public ActionResponse unmanage(String shareServerId, boolean force) {
        return action(MANAGE, server(shareServerId), "unmanage", Map.of("force", force));
    }

    @Override
    public ActionResponse resetStatus(String shareServerId, String status) {
        return action(MANAGE, server(shareServerId), "reset_status", Map.of("status", Objects.requireNonNull(status, "status")));
    }

    @Override
    public Map<String, Object> migrationCheck(String shareServerId, Map<String, ?> migration) {
        return actionResult(MIGRATION, server(shareServerId), "migration_check", Objects.requireNonNull(migration, "migration"), true);
    }

    @Override
    public ActionResponse migrationStart(String shareServerId, Map<String, ?> migration) {
        return experimental(MIGRATION, NEVER_STABLE, server(shareServerId), "migration_start", Objects.requireNonNull(migration, "migration"));
    }

    @Override
    public Map<String, Object> migrationProgress(String shareServerId) {
        return actionResult(MIGRATION, server(shareServerId), "migration_get_progress", null, true);
    }

    @Override
    public ActionResponse migrationComplete(String shareServerId) {
        return experimental(MIGRATION, NEVER_STABLE, server(shareServerId), "migration_complete", null);
    }

    @Override
    public ActionResponse migrationCancel(String shareServerId) {
        return experimental(MIGRATION, NEVER_STABLE, server(shareServerId), "migration_cancel", null);
    }

    @Override
    public ActionResponse resetTaskState(String shareServerId, String taskState) {
        Map<String, Object> body = new HashMap<>();
        body.put("task_state", taskState);
        return experimental(MIGRATION, NEVER_STABLE, server(shareServerId), "reset_task_state", body);
    }

    @Override
    public ActionResponse updateSecurityService(String shareNetworkId, String currentServiceId, String newServiceId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("current_service_id", Objects.requireNonNull(currentServiceId, "currentServiceId"));
        body.put("new_service_id", Objects.requireNonNull(newServiceId, "newServiceId"));
        return action(SECURITY, network(shareNetworkId), "update_security_service", body);
    }

    @Override
    public Map<String, Object> checkUpdateSecurityService(String shareNetworkId, String currentServiceId, String newServiceId, boolean resetOperation) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("current_service_id", Objects.requireNonNull(currentServiceId, "currentServiceId"));
        body.put("new_service_id", Objects.requireNonNull(newServiceId, "newServiceId"));
        body.put("reset_operation", resetOperation);
        return actionResult(SECURITY, network(shareNetworkId), "update_security_service_check", body, false);
    }

    @Override
    public Map<String, Object> checkAddSecurityService(String shareNetworkId, String securityServiceId, boolean resetOperation) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("security_service_id", Objects.requireNonNull(securityServiceId, "securityServiceId"));
        body.put("reset_operation", resetOperation);
        return actionResult(SECURITY, network(shareNetworkId), "add_security_service_check", body, false);
    }

    @Override
    public ActionResponse resetShareNetworkStatus(String shareNetworkId, String status) {
        return action(SECURITY, network(shareNetworkId), "reset_status", Map.of("status", Objects.requireNonNull(status, "status")));
    }

    /** An action whose response body is the result. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> actionResult(MicroVersion floor, String path, String name, Map<String, ?> body, boolean experimental) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(name, body);
        String feature = path + "/action " + name;
        Invocation<Map> invocation = at(floor, post(Map.class, path + "/action"), feature);
        if (experimental)
            experimentalHeader(floor, NEVER_STABLE, invocation, feature);
        Map<String, Object> result = invocation.entity(JsonBody.of(wrapper)).execute(propagate404());
        return result == null ? new LinkedHashMap<>() : result;
    }
}
