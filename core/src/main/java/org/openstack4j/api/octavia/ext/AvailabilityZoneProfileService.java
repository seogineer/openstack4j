package org.openstack4j.api.octavia.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.AvailabilityZoneProfile;
import org.openstack4j.model.octavia.options.AvailabilityZoneProfileOptions;

/**
 * Octavia availability zone profiles ({@code /v2/lbaas/availabilityzoneprofiles}).
 */
public interface AvailabilityZoneProfileService extends RestService {

    /**
     * Lists availability zone profiles (admin).
     *
     * @return the result
     */
    List<? extends AvailabilityZoneProfile> list();

    /**
     * Lists availability zone profiles (admin).
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends AvailabilityZoneProfile> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    AvailabilityZoneProfile get(String id);

    /**
     * Creates an availability zone profile (admin).
     *
     * @param options the options
     * @return the result
     */
    AvailabilityZoneProfile create(AvailabilityZoneProfileOptions options);

    /**
     * Updates a profile; only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    AvailabilityZoneProfile update(String id, AvailabilityZoneProfileOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
