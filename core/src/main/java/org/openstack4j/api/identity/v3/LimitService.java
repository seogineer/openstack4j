package org.openstack4j.api.identity.v3;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Limit;
import org.openstack4j.model.identity.v3.LimitModel;
import org.openstack4j.model.identity.v3.options.LimitCreate;
import org.openstack4j.model.identity.v3.options.LimitListOptions;

/**
 * Unified limits: project and domain limits and the enforcement model ({@code /v3/limits}).
 */
public interface LimitService extends RestService {

    /**
     * Lists limits.
     *
     * @return the result
     */
    List<? extends Limit> list();

    /**
     * Lists limits, optionally filtered.
     *
     * @param options the options
     * @return the result
     */
    List<? extends Limit> list(LimitListOptions options);

    /**
     * @param id the id
     * @return the result
     */
    Limit get(String id);

    /**
     * Creates registered limits in one request.
     *
     * @param limits the limits
     * @return the result
     */
    List<? extends Limit> create(List<LimitCreate> limits);

    /**
     * Updates a limit; null arguments are left unchanged.
     *
     * @param id the id
     * @param resourceLimit the resource limit
     * @param description the description
     * @return the result
     */
    Limit update(String id, Integer resourceLimit, String description);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);

    /**
     * Returns the enforcement model.
     *
     * @return the result
     */
    LimitModel model();
}
