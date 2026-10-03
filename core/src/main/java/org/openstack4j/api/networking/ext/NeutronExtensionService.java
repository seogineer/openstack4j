package org.openstack4j.api.networking.ext;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.network.ext.NeutronExtension;

/**
 * Neutron API extensions ({@code GET /v2.0/extensions}): which optional features this deployment has.
 */
public interface NeutronExtensionService extends RestService {

    /**
     * Lists the API extensions this Neutron enables.
     *
     * @return the result
     */
    List<? extends NeutronExtension> list();

    /**
     * Returns one extension by alias.
     *
     * @param alias the alias
     * @return the result
     */
    NeutronExtension get(String alias);

    /**
     * Checks whether an extension alias is enabled (one list request).
     *
     * @param alias the alias
     * @return the result
     */
    boolean isEnabled(String alias);
}
