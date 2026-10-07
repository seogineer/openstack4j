package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.NetworkLog;
import org.openstack4j.model.network.options.NetworkLogOptions;

/**
 * Network logging ({@code /v2.0/log}, logging).
 */
public interface NetworkLoggingService extends RestService {

    /**
     * Lists network logs.
     *
     * @return the result
     */
    List<? extends NetworkLog> list();

    /**
     * Lists network logs, optionally filtered.
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends NetworkLog> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    NetworkLog get(String id);

    /**
     * Creates a network log for a resource type (security_group, firewall_group).
     *
     * @param options the options
     * @return the result
     */
    NetworkLog create(NetworkLogOptions options);

    /**
     * Updates a log; only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    NetworkLog update(String id, NetworkLogOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);

    /**
     * Lists the resource types that can be logged.
     *
     * @return the result
     */
    List<String> loggableResources();
}
