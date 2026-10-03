package org.openstack4j.api.identity.v3;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.RegisteredLimit;
import org.openstack4j.model.identity.v3.options.RegisteredLimitCreate;
import org.openstack4j.model.identity.v3.options.RegisteredLimitListOptions;

/**
 * Unified limits: registered (default) limits ({@code /v3/registered_limits}).
 */
public interface RegisteredLimitService extends RestService {

    /**
     * Lists registered limits, optionally filtered.
     *
     * @return the result
     */
    List<? extends RegisteredLimit> list();

    /**
     * Lists registered limits, optionally filtered.
     *
     * @param options the options
     * @return the result
     */
    List<? extends RegisteredLimit> list(RegisteredLimitListOptions options);

    /**
     * @param id the id
     * @return the result
     */
    RegisteredLimit get(String id);

    /**
     * Creates registered limits in one request.
     *
     * @param limits the limits
     * @return the result
     */
    List<? extends RegisteredLimit> create(List<RegisteredLimitCreate> limits);

    /**
     * Updates a registered limit; null arguments are left unchanged.
     *
     * @param id the id
     * @param defaultLimit the default limit
     * @param description the description
     * @return the result
     */
    RegisteredLimit update(String id, Integer defaultLimit, String description);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
