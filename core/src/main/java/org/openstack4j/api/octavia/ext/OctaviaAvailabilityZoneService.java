package org.openstack4j.api.octavia.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.OctaviaAvailabilityZone;
import org.openstack4j.model.octavia.options.OctaviaAvailabilityZoneOptions;

/**
 * Octavia availability zones ({@code /v2/lbaas/availabilityzones}), addressed by name.
 */
public interface OctaviaAvailabilityZoneService extends RestService {

    /**
     * Lists availability zones.
     *
     * @return the result
     */
    List<? extends OctaviaAvailabilityZone> list();

    /**
     * Lists availability zones, optionally filtered.
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends OctaviaAvailabilityZone> list(Map<String, String> filters);

    /**
     * Returns an availability zone by name; null if missing.
     *
     * @param name the name
     * @return the result
     */
    OctaviaAvailabilityZone get(String name);

    /**
     * Creates an availability zone (admin).
     *
     * @param options the options
     * @return the result
     */
    OctaviaAvailabilityZone create(OctaviaAvailabilityZoneOptions options);

    /**
     * Updates an availability zone; only the fields set are sent.
     *
     * @param name the name
     * @param options the options
     * @return the result
     */
    OctaviaAvailabilityZone update(String name, OctaviaAvailabilityZoneOptions options);

    /**
     * Deletes an availability zone by name.
     *
     * @param name the name
     * @return the action response
     */
    ActionResponse delete(String name);
}
