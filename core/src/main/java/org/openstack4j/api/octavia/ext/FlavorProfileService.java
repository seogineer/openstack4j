package org.openstack4j.api.octavia.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.FlavorProfile;
import org.openstack4j.model.octavia.options.FlavorProfileOptions;

/**
 * Octavia flavor profiles ({@code /v2/lbaas/flavorprofiles}).
 */
public interface FlavorProfileService extends RestService {

    /**
     * Lists flavor profiles (admin).
     *
     * @return the result
     */
    List<? extends FlavorProfile> list();

    /**
     * Lists flavor profiles (admin).
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends FlavorProfile> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    FlavorProfile get(String id);

    /**
     * Creates a flavor profile (admin).
     *
     * @param options the options
     * @return the result
     */
    FlavorProfile create(FlavorProfileOptions options);

    /**
     * Updates a flavor profile; only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    FlavorProfile update(String id, FlavorProfileOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
