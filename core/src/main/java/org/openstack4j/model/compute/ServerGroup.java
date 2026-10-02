package org.openstack4j.model.compute;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/**
 * An OpenStack ServerGroup which is a group that the server in it must be Located on different hosts
 *
 * @author Octopus Zhang
 */
public interface ServerGroup extends ModelEntity {

    /**
     * @return the id of this group
     */
    String getId();

    /**
     * @return the name of this group
     */
    String getName();

    /**
     * @return the servers in this group
     */
    List<String> getMembers();

    /**
     * @return the metadata of this group
     */
    Map<String, String> getMetadata();

    /**
     * @return the polices of this group
     */
    List<String> getPolicies();

    /** @return the single policy (2.64+) */
    default String getPolicy() { return null; }
    /** @return policy rules such as max_server_per_host (2.64+) */
    default Map<String, Object> getRules() { return null; }
    /** @return project_id (2.13+) */
    default String getProjectId() { return null; }
    /** @return user_id (2.13+) */
    default String getUserId() { return null; }
}
