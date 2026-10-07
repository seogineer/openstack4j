package org.openstack4j.openstack.baremetal.internal;

import org.openstack4j.api.baremetal.BaremetalMicroVersionService;
import org.openstack4j.model.baremetal.BaremetalVersion;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.internal.microversion.VersionRange;
import org.openstack4j.openstack.baremetal.domain.IronicBaremetalVersion;
import org.openstack4j.openstack.baremetal.domain.IronicVersions;

public class BaremetalMicroVersionServiceImpl extends BaseBaremetalServices implements BaremetalMicroVersionService {

    @Override
    public BaremetalVersion negotiate() {
        BaremetalMicroVersions.SUPPORT.negotiate(BaremetalMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public BaremetalVersion use(String version) {
        BaremetalMicroVersions.SUPPORT.use(version, BaremetalMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public void clear() {
        BaremetalMicroVersions.SUPPORT.clear();
    }

    @Override
    public BaremetalVersion get() {
        MicroVersionState state = BaremetalMicroVersions.currentState();
        if (state == null)
            return new IronicBaremetalVersion(null, null, null, false, false);
        MicroVersion effective = effectiveMicroVersion(null);
        return new IronicBaremetalVersion(state.getServerMin().toString(), state.getServerMax().toString(),
                effective == null ? null : effective.toString(), state.getPinned() != null, state.isEnabled());
    }

    /** @return the v1 range from the Ironic root document, or {@code null} when the server has none */
    private static VersionRange discover() {
        IronicVersions.Entry v1 = new BaremetalVersionDiscovery().fetch().v1();
        if (v1 == null || v1.version == null || v1.version.isEmpty())
            return null;
        MicroVersion min = v1.minVersion == null || v1.minVersion.isEmpty() ? null : MicroVersions.parse(v1.minVersion);
        return new VersionRange(min, MicroVersions.parse(v1.version));
    }
}
