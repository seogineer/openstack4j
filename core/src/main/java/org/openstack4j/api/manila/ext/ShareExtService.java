package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.Access;
import org.openstack4j.model.manila.ShareInstance;
import org.openstack4j.model.manila.ext.ExportLocation;
import org.openstack4j.model.manila.ext.ShareAccessRule;
import org.openstack4j.model.manila.ext.ShareInfo;
import org.openstack4j.model.manila.ext.options.ShareAccessCreate;
import org.openstack4j.model.manila.ext.options.ShareMigration;

/** The 2.7+ share APIs: export locations, instances, manage, revert, soft delete, access rules and migration. */
public interface ShareExtService extends RestService {

    /** @return the export locations of a share (2.9); a missing share raises */
    List<? extends ExportLocation> listExportLocations(String shareId);

    /** @return the export location, or {@code null} when it does not exist (2.9) */
    ExportLocation getExportLocation(String shareId, String exportLocationId);

    /** @return the metadata of an export location (2.87); a missing one raises */
    Map<String, String> getExportLocationMetadata(String shareId, String exportLocationId);

    /** @return one metadata value of an export location, or {@code null} when it is not set (2.87) */
    String getExportLocationMetadataItem(String shareId, String exportLocationId, String key);

    /** Adds or changes the given metadata items (2.87) and returns all of them. */
    Map<String, String> setExportLocationMetadata(String shareId, String exportLocationId, Map<String, String> metadata);

    /** Replaces all metadata of an export location (2.87) and returns it. */
    Map<String, String> replaceExportLocationMetadata(String shareId, String exportLocationId, Map<String, String> metadata);

    ActionResponse deleteExportLocationMetadataItem(String shareId, String exportLocationId, String key);

    /** @return the export locations of a share instance (2.9, admin); a missing instance raises */
    List<? extends ExportLocation> listInstanceExportLocations(String shareInstanceId);

    /** @return the instances of a share (2.7, admin); a missing share raises */
    List<? extends ShareInstance> listInstances(String shareId);

    /**
     * Brings an existing back-end share under Manila ({@code POST /shares/manage}, 2.7, admin).
     *
     * @param share the {@code share} fields: {@code protocol}, {@code export_path}, {@code service_host} and optional
     *              {@code name}, {@code share_type}, {@code driver_options}, {@code is_public}, {@code description} ...
     */
    ShareInfo manage(Map<String, ?> share);

    /** Removes a share from Manila without deleting it on the back end (2.7, admin). */
    ActionResponse unmanage(String shareId);

    /** Reverts a share to its latest snapshot (2.27). */
    ActionResponse revertToSnapshot(String shareId, String snapshotId);

    /** Moves a share to the recycle bin (2.69). */
    ActionResponse softDelete(String shareId);

    /** Restores a share from the recycle bin (2.69). */
    ActionResponse restore(String shareId);

    /** Adds an access rule (2.7; metadata 2.45, locks 2.82). */
    Access grantAccess(String shareId, ShareAccessCreate access);

    /** Removes an access rule (2.7). */
    ActionResponse revokeAccess(String shareId, String accessId);

    /** @param filters query parameters such as {@code access_type}, {@code access_level}, {@code metadata} @return the access rules of a share (2.45) */
    List<? extends ShareAccessRule> listAccessRules(String shareId, Map<String, String> filters);

    /** @return the access rule, or {@code null} when it does not exist (2.45) */
    ShareAccessRule getAccessRule(String accessId);

    /** Changes the access level, {@code rw} or {@code ro} (2.88). */
    ShareAccessRule updateAccessRuleLevel(String accessId, String accessLevel);

    /** Adds or changes metadata items of an access rule (2.45) and returns all of them. */
    Map<String, String> updateAccessRuleMetadata(String accessId, Map<String, String> metadata);

    ActionResponse deleteAccessRuleMetadata(String accessId, String key);

    /** Starts migrating a share (2.29; experimental below 2.96, the header is sent for you). */
    ActionResponse migrationStart(String shareId, ShareMigration migration);

    /** @return {@code total_progress} and {@code task_state} (2.29) */
    Map<String, Object> migrationProgress(String shareId);

    ActionResponse migrationComplete(String shareId);

    ActionResponse migrationCancel(String shareId);
}
