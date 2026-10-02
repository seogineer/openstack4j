package org.openstack4j.api.compute;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.compute.ExternalEvent;
import org.openstack4j.model.compute.ExternalEventCreate;

/**
 * Server external events ({@code POST /os-server-external-events}). Normally used by Neutron, Cinder and Cyborg;
 * admin only.
 */
public interface ServerExternalEventService extends RestService {

    /**
     * Sends events to servers. {@code volume-extended} needs 2.51, {@code power-update} 2.76 and
     * {@code accelerator-request-bound} 2.82.
     *
     * @return the per-event results
     */
    List<? extends ExternalEvent> create(List<ExternalEventCreate> events);
}
