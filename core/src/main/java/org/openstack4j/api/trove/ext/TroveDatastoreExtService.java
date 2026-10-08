package org.openstack4j.api.trove.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/** Datastore versions by id, their configuration parameters, datastore deletion and the project's limits. */
public interface TroveDatastoreExtService extends RestService {

    /** Deletes a datastore that has no versions left (admin). */
    ActionResponse deleteDatastore(String datastore);

    /** @return a datastore version by its id; a missing version raises */
    Map<String, Object> getVersion(String versionId);

    /** @return the configuration parameters of a datastore version ({@code name}, {@code type}, {@code min}, {@code max} …) */
    List<Map<String, Object>> listParameters(String datastore, String version);

    /** @return one configuration parameter; a missing one raises */
    Map<String, Object> getParameter(String datastore, String version, String parameterName);

    /** @return the configuration parameters of a datastore version given by id */
    List<Map<String, Object>> listParameters(String versionId);

    Map<String, Object> getParameter(String versionId, String parameterName);

    /** @return the absolute limits and rate limits of the project ({@code GET /limits}) */
    List<Map<String, Object>> limits();
}
