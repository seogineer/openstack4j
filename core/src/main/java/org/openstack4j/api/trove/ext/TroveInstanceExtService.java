package org.openstack4j.api.trove.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.trove.Instance;

/** Database instance updates, actions, logs, SSL and root access. */
public interface TroveInstanceExtService extends RestService {

    /** @return the instances with all fields */
    List<? extends Instance> listDetail();

    /** @param filters query parameters such as {@code include_clustered}, {@code project_id} (admin) */
    List<? extends Instance> listDetail(Map<String, String> filters);

    ActionResponse rename(String instanceId, String name);

    /** Attaches a configuration group (a restart may be needed). */
    ActionResponse attachConfiguration(String instanceId, String configurationId);

    ActionResponse detachConfiguration(String instanceId);

    /** Upgrades the instance to another version of its datastore. */
    ActionResponse upgradeDatastoreVersion(String instanceId, String datastoreVersion);

    /** Makes a replica a stand-alone instance. */
    ActionResponse detachReplica(String instanceId);

    /**
     * @param isPublic     whether the database is reachable from outside the project network
     * @param allowedCidrs the CIDRs allowed to connect; {@code null} leaves it out
     */
    ActionResponse updateAccess(String instanceId, boolean isPublic, List<String> allowedCidrs);

    /** Restarts the database service. */
    ActionResponse restart(String instanceId);

    ActionResponse resizeFlavor(String instanceId, String flavorId);

    /** @param sizeGb the new volume size in GB */
    ActionResponse resizeVolume(String instanceId, int sizeGb);

    /** Promotes a replica to replication source. */
    ActionResponse promoteToReplicaSource(String instanceId);

    /** Ejects a failed replication source; a replica becomes the new source. */
    ActionResponse ejectReplicaSource(String instanceId);

    /** Resets the instance status to ERROR (admin). */
    ActionResponse resetStatus(String instanceId);

    /** @return the backups of an instance; a missing instance raises */
    List<Map<String, Object>> listBackups(String instanceId);

    /** @return the configuration parameter defaults of an instance; a missing instance raises */
    Map<String, Object> configurationDefaults(String instanceId);

    /** @return the guest logs of an instance ({@code name}, {@code type}, {@code status} …); a missing instance raises */
    List<Map<String, Object>> listLogs(String instanceId);

    /** @return the log details */
    Map<String, Object> showLog(String instanceId, String logName);

    Map<String, Object> enableLog(String instanceId, String logName);

    Map<String, Object> disableLog(String instanceId, String logName);

    /** Publishes the log to object storage. */
    Map<String, Object> publishLog(String instanceId, String logName);

    /** Deletes the published log from object storage. */
    Map<String, Object> discardLog(String instanceId, String logName);

    /** @return the SSL {@code status}, {@code mode} and {@code certificate}; a missing instance raises */
    Map<String, Object> sslStatus(String instanceId);

    /** @param options e.g. {@code mode}, {@code container_ref}, {@code password_ref} @return the SSL status */
    Map<String, Object> enableSsl(String instanceId, Map<String, ?> options);

    Map<String, Object> disableSsl(String instanceId);

    /** Rolls back a failed SSL change. */
    Map<String, Object> rollbackSsl(String instanceId);

    /** @return whether root access was ever enabled */
    boolean isRootEnabled(String instanceId);

    /** @param password the root password, or {@code null} to let Trove generate one @return {@code name} and {@code password} */
    Map<String, Object> enableRoot(String instanceId, String password);

    ActionResponse disableRoot(String instanceId);
}
