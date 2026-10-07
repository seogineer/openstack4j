package org.openstack4j.api.octavia.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.Amphora;
import org.openstack4j.model.octavia.ext.AmphoraStats;

/**
 * Octavia amphorae ({@code /v2/octavia/amphorae}; admin, amphora driver only).
 */
public interface AmphoraService extends RestService {

    /**
     * Lists amphorae, optionally filtered (load_balancer_id, status).
     *
     * @return the result
     */
    List<? extends Amphora> list();

    /**
     * Lists amphorae, optionally filtered (load_balancer_id, status).
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends Amphora> list(Map<String, String> filters);

    /**
     * Returns an amphora; null if missing.
     *
     * @param id the id
     * @return the result
     */
    Amphora get(String id);

    /**
     * Returns per-listener statistics of an amphora.
     *
     * @param id the id
     * @return the result
     */
    List<? extends AmphoraStats> stats(String id);

    /**
     * Pushes the current configuration to the amphora agent.
     *
     * @param id the id
     * @return the action response
     */
    ActionResponse configure(String id);

    /**
     * Replaces the amphora with a new one.
     *
     * @param id the id
     * @return the action response
     */
    ActionResponse failover(String id);

    /**
     * Deletes a spare or error amphora.
     *
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
