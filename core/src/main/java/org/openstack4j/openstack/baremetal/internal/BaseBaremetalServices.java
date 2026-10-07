package org.openstack4j.openstack.baremetal.internal;

import org.openstack4j.openstack.common.functions.EnforceVersionToURL;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.BaseOpenStackService;
import org.openstack4j.openstack.internal.MicroVersion;

/**
 * Base Ironic service layer. Adds the bare metal microversion header when the session turned microversions on.
 *
 * @author Jeremy Unruh
 */
public class BaseBaremetalServices extends BaseOpenStackService {

    public BaseBaremetalServices() {
        super(ServiceType.BAREMETAL, EnforceVersionToURL.instance("/v1"));
    }

    /** Highest microversion every API of this service supports, or {@code null} for no limit. */
    protected MicroVersion classCeiling() {
        return null;
    }

    @Override
    protected <R> Invocation<R> decorate(Invocation<R> invocation) {
        return capped(invocation, null);
    }

    /** Sends this request at no more than {@code ceiling}. */
    protected <R> Invocation<R> capped(Invocation<R> invocation, MicroVersion ceiling) {
        MicroVersion version = effectiveMicroVersion(ceiling);
        if (version != null)
            BaremetalMicroVersions.SUPPORT.headers(version).forEach(invocation::header);
        return invocation;
    }

    /** @return the microversion a request with {@code ceiling} would carry, or {@code null} when microversions are off */
    protected MicroVersion effectiveMicroVersion(MicroVersion ceiling) {
        return BaremetalMicroVersions.SUPPORT.effective(classCeiling(), ceiling);
    }

    protected boolean isMicroVersionAtLeast(MicroVersion version) {
        MicroVersion effective = effectiveMicroVersion(null);
        return effective != null && effective.compareTo(version) >= 0;
    }

    /** Fails before any request when {@code feature} needs a microversion the session does not send. */
    protected void requireMicroVersion(String feature, MicroVersion floor) {
        BaremetalMicroVersions.SUPPORT.require(feature, floor, effectiveMicroVersion(null));
    }
}
