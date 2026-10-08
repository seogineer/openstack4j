package org.openstack4j.api.trove.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Trove management (admin) APIs: {@code /mgmt/instances}, {@code /mgmt/datastore-versions}, parameters and quotas. */
public interface TroveAdminService extends RestService {

    /** @param filters query parameters such as {@code deleted}, {@code include_clustered}, {@code project_id} @return all instances with server details */
    List<Map<String, Object>> listInstances(Map<String, String> filters);

    /** @return an instance with server details; a missing instance raises */
    Map<String, Object> getInstance(String instanceId);

    /** Stops the database service. */
    ActionResponse stop(String instanceId);

    /** Reboots the instance's server. */
    ActionResponse reboot(String instanceId);

    /** @param host the target compute host, or {@code null} to let the scheduler choose */
    ActionResponse migrate(String instanceId, String host);

    ActionResponse resetTaskStatus(String instanceId);

    /** Rebuilds the instance's server from a guest image. */
    ActionResponse rebuild(String instanceId, String imageId);

    /** @return when and by whom root was enabled ({@code enabled}, {@code user}); a missing instance raises */
    Map<String, Object> rootHistory(String instanceId);

    /** @return all datastore versions with management fields */
    List<Map<String, Object>> listDatastoreVersions();

    /** @return a datastore version with management fields; a missing one raises */
    Map<String, Object> getDatastoreVersion(String versionId);

    /** @param version {@code datastore_name}, {@code datastore_manager}, {@code name}, {@code image} or {@code image_tags}, {@code active} … */
    ActionResponse createDatastoreVersion(Map<String, ?> version);

    /** @param fields e.g. {@code name}, {@code image}, {@code active}, {@code default} */
    ActionResponse updateDatastoreVersion(String versionId, Map<String, ?> fields);

    ActionResponse deleteDatastoreVersion(String versionId);

    /**
     * Adds a configuration parameter to a datastore version.
     *
     * @param parameter {@code name}, {@code data_type}, {@code min_size}, {@code max_size}, {@code restart_required}
     * @return the version's parameters
     */
    List<Map<String, Object>> createParameter(String versionId, Map<String, ?> parameter);

    /** @return the updated parameter */
    Map<String, Object> updateParameter(String versionId, String parameterName, Map<String, ?> parameter);

    ActionResponse deleteParameter(String versionId, String parameterName);

    /** @return the quotas of a project ({@code resource}, {@code in_use}, {@code limit}, {@code reserved}) */
    List<Map<String, Object>> getQuotas(String projectId);

    /** Changes quotas of a project, e.g. {@code instances}, {@code backups}, {@code volumes}, and returns them. */
    Map<String, Object> updateQuotas(String projectId, Map<String, Integer> quotas);
}
