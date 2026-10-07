package org.openstack4j.api.manila.ext;

import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareServerInfo;

/**
 * The 2.49+ share server and share network APIs: server details, manage/unmanage, reset status, server migration
 * (experimental, the header is sent for you) and share network security service updates.
 */
public interface ShareServerExtService extends RestService {

    /** @return the back-end details of a share server (admin); a missing server raises */
    Map<String, Object> details(String shareServerId);

    /**
     * Brings an existing back-end share server under Manila (2.49, admin).
     *
     * @param shareServer the {@code share_server} fields: {@code host}, {@code share_network_id}, {@code identifier} and
     *                    optional {@code share_network_subnet_id} (2.51), {@code driver_options}
     */
    ShareServerInfo manage(Map<String, ?> shareServer);

    /** @param force removes the server even when the driver fails (2.49, admin) */
    ActionResponse unmanage(String shareServerId, boolean force);

    /** @param status e.g. {@code active}, {@code error} (2.49, admin) */
    ActionResponse resetStatus(String shareServerId, String status);

    /**
     * Checks whether a share server can be migrated (2.57, admin).
     *
     * @param migration {@code host}, {@code writable}, {@code nondisruptive}, {@code preserve_snapshots}, {@code new_share_network_id}
     * @return {@code compatible}, {@code requested_capabilities} and {@code supported_capabilities}
     */
    Map<String, Object> migrationCheck(String shareServerId, Map<String, ?> migration);

    /** Starts migrating a share server (2.57, admin); same fields as {@link #migrationCheck}. */
    ActionResponse migrationStart(String shareServerId, Map<String, ?> migration);

    /** @return {@code total_progress}, {@code task_state} and {@code destination_share_server_id} (2.57) */
    Map<String, Object> migrationProgress(String shareServerId);

    ActionResponse migrationComplete(String shareServerId);

    ActionResponse migrationCancel(String shareServerId);

    /** @param taskState e.g. {@code migration_error}, or {@code null} to clear it (2.57, admin) */
    ActionResponse resetTaskState(String shareServerId, String taskState);

    /** Replaces a security service of a share network that is in use (2.63). */
    ActionResponse updateSecurityService(String shareNetworkId, String currentServiceId, String newServiceId);

    /** @return whether the replacement can be done ({@code compatible}, {@code requested_operation} ...; 2.63) */
    Map<String, Object> checkUpdateSecurityService(String shareNetworkId, String currentServiceId, String newServiceId, boolean resetOperation);

    /** @return whether a security service can be added to a share network in use (2.63) */
    Map<String, Object> checkAddSecurityService(String shareNetworkId, String securityServiceId, boolean resetOperation);

    /** @param status e.g. {@code active}, {@code error} (2.63, admin) */
    ActionResponse resetShareNetworkStatus(String shareNetworkId, String status);
}
