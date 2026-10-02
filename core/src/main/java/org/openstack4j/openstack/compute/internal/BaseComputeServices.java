package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.compute.domain.actions.ServerAction;
import org.openstack4j.openstack.compute.functions.ToActionResponseFunction;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.MicroVersion;

/**
 * Base class for Computer / Nova services. Adds compute microversion headers when the session turned microversions on.
 *
 * @author Jeremy Unruh
 */
public class BaseComputeServices extends BaseOpenStackService {

    protected BaseComputeServices() {
        super(ServiceType.COMPUTE);
    }

    /** Highest microversion every API of this service supports, or {@code null} for no limit. */
    protected MicroVersion classCeiling() {
        return null;
    }

    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        return capped(invocation, null);
    }

    protected <R> Invocation<R> capped(Invocation<R> invocation, MicroVersion ceiling) {
        MicroVersion version = effectiveMicroVersion(ceiling);
        if (version != null)
            ComputeMicroVersions.SUPPORT.headers(version).forEach(invocation::header);
        return invocation;
    }

    protected MicroVersion effectiveMicroVersion(MicroVersion ceiling) {
        return ComputeMicroVersions.SUPPORT.effective(classCeiling(), ceiling);
    }

    protected boolean isMicroVersionAtLeast(MicroVersion version) {
        MicroVersion effective = effectiveMicroVersion(null);
        return effective != null && effective.compareTo(version) >= 0;
    }

    protected void requireMicroVersion(String feature, MicroVersion floor) {
        ComputeMicroVersions.SUPPORT.require(feature, floor, effectiveMicroVersion(null));
    }

    protected ActionResponse invokeAction(String serverId, ServerAction action) {
        return ToActionResponseFunction.INSTANCE.apply(invokeActionWithResponse(serverId, action), action.getClass().getName());
    }

    protected ActionResponse invokeAction(String serverId, ServerAction action, MicroVersion ceiling) {
        return ToActionResponseFunction.INSTANCE.apply(invokeActionWithResponse(serverId, action, ceiling), action.getClass().getName());
    }

    protected HttpResponse invokeActionWithResponse(String serverId, ServerAction action) {
        return invokeActionWithResponse(serverId, action, null);
    }

    protected HttpResponse invokeActionWithResponse(String serverId, ServerAction action, MicroVersion ceiling) {
        return capped(post(Void.class, uri("/servers/%s/action", serverId)), ceiling)
                .entity(action)
                .executeWithResponse();
    }
}
