package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.compute.domain.actions.ServerAction;
import org.openstack4j.openstack.compute.functions.ToActionResponseFunction;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersions;

/**
 * Base class for Computer / Nova services. Adds compute microversion headers when the session turned microversions on.
 *
 * @author Jeremy Unruh
 */
public class BaseComputeServices extends BaseOpenStackService {

    static final String API_VERSION_HEADER = "OpenStack-API-Version";
    static final String NOVA_VERSION_HEADER = "X-OpenStack-Nova-API-Version";

    protected BaseComputeServices() {
        super(ServiceType.COMPUTE);
    }

    /** Highest microversion every API of this service supports, or {@code null} for no limit. */
    protected MicroVersion classCeiling() {
        return null;
    }

    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        MicroVersion version = effectiveMicroVersion(null);
        if (version != null)
            setVersionHeaders(invocation, version);
        return invocation;
    }

    /** Sends this request at no more than {@code ceiling}. */
    protected <R> Invocation<R> capped(Invocation<R> invocation, MicroVersion ceiling) {
        MicroVersion version = effectiveMicroVersion(ceiling);
        if (version != null)
            setVersionHeaders(invocation, version);
        return invocation;
    }

    /** @return the microversion a request with {@code ceiling} would carry, or {@code null} when microversions are off */
    protected MicroVersion effectiveMicroVersion(MicroVersion ceiling) {
        MicroVersionState state = ComputeMicroVersions.currentState();
        if (state == null || !state.isEnabled())
            return null;
        MicroVersion version = state.getPinned() != null ? state.getPinned()
                : MicroVersions.min(ComputeMicroVersions.LATEST, state.getServerMax());
        MicroVersion classCeiling = classCeiling();
        if (classCeiling != null)
            version = MicroVersions.min(version, classCeiling);
        if (ceiling != null)
            version = MicroVersions.min(version, ceiling);
        return version;
    }

    protected boolean isMicroVersionAtLeast(MicroVersion version) {
        MicroVersion effective = effectiveMicroVersion(null);
        return effective != null && effective.compareTo(version) >= 0;
    }

    /** Fails before any request when {@code feature} needs a microversion the session does not send. */
    protected void requireMicroVersion(String feature, MicroVersion floor) {
        MicroVersion effective = effectiveMicroVersion(null);
        if (effective == null)
            throw new MicroVersionException(feature + " requires compute microversion " + floor
                    + "; turn microversions on with os.compute().microVersions().negotiate()");
        if (effective.compareTo(floor) < 0) {
            MicroVersionState state = ComputeMicroVersions.currentState();
            throw new MicroVersionException(String.format(
                    "%s requires compute microversion %s, but the session sends %s (server max %s)",
                    feature, floor, effective, state == null ? "?" : state.getServerMax()));
        }
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

    private static <R> void setVersionHeaders(Invocation<R> invocation, MicroVersion version) {
        invocation.header(API_VERSION_HEADER, "compute " + version);
        invocation.header(NOVA_VERSION_HEADER, version.toString());
    }
}
