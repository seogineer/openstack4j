package org.openstack4j.openstack.networking.internal.ext;

import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;

/**
 * Execution options of the Neutron extension calls: a 404 means the extension is disabled or the parent resource is
 * missing, so it is raised instead of being turned into an empty list or null. Single {@code get(id)} calls keep the
 * openstack4j convention of returning null for a missing resource and do not use this.
 */
public final class NeutronExecution {

    private NeutronExecution() {
    }

    public static <T> ExecutionOptions<T> propagate404() {
        return ExecutionOptions.create(PropagateOnStatus.on(404));
    }
}
