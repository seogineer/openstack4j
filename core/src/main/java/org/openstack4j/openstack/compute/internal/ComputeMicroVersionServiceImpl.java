package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.compute.ComputeMicroVersionService;
import org.openstack4j.model.compute.ComputeVersion;
import org.openstack4j.openstack.compute.domain.NovaComputeVersion;
import org.openstack4j.openstack.compute.domain.NovaVersions;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.internal.microversion.VersionRange;

public class ComputeMicroVersionServiceImpl extends BaseComputeServices implements ComputeMicroVersionService {

    @Override
    public ComputeVersion negotiate() {
        ComputeMicroVersions.SUPPORT.negotiate(ComputeMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public ComputeVersion use(String version) {
        ComputeMicroVersions.SUPPORT.use(version, ComputeMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public void clear() {
        ComputeMicroVersions.SUPPORT.clear();
    }

    @Override
    public ComputeVersion get() {
        MicroVersionState state = ComputeMicroVersions.currentState();
        if (state == null)
            return new NovaComputeVersion(null, null, null, false, false);
        MicroVersion effective = effectiveMicroVersion(null);
        return new NovaComputeVersion(state.getServerMin().toString(), state.getServerMax().toString(),
                effective == null ? null : effective.toString(), state.getPinned() != null, state.isEnabled());
    }

    /** @return the v2.1 range from the Nova root document, or {@code null} when the server has none */
    private static VersionRange discover() {
        NovaVersions.Entry v21 = new ComputeVersionDiscovery().fetch().v21();
        if (v21 == null || v21.version == null || v21.version.isEmpty())
            return null;
        MicroVersion min = v21.minVersion == null || v21.minVersion.isEmpty() ? null : MicroVersions.parse(v21.minVersion);
        return new VersionRange(min, MicroVersions.parse(v21.version));
    }
}
