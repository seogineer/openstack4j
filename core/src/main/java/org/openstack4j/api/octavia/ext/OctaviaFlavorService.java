package org.openstack4j.api.octavia.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.OctaviaFlavor;
import org.openstack4j.model.octavia.options.OctaviaFlavorOptions;

/**
 * Octavia flavors ({@code /v2/lbaas/flavors}); not compute flavors.
 */
public interface OctaviaFlavorService extends RestService {

    /**
     * Lists Octavia flavors, optionally filtered.
     *
     * @return the result
     */
    List<? extends OctaviaFlavor> list();

    /**
     * Lists Octavia flavors, optionally filtered.
     *
     * @param Map<String the map< string
     * @param filters the filters
     * @return the result
     */
    List<? extends OctaviaFlavor> list(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    OctaviaFlavor get(String id);

    /**
     * Creates a flavor (admin).
     *
     * @param options the options
     * @return the result
     */
    OctaviaFlavor create(OctaviaFlavorOptions options);

    /**
     * Updates a flavor; only the fields set are sent.
     *
     * @param id the id
     * @param options the options
     * @return the result
     */
    OctaviaFlavor update(String id, OctaviaFlavorOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse delete(String id);
}
