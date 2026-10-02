package org.openstack4j.openstack.compute.internal;

import org.openstack4j.api.compute.ComputeMicroVersionService;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.model.compute.ComputeVersion;
import org.openstack4j.openstack.compute.domain.NovaComputeVersion;
import org.openstack4j.openstack.compute.domain.NovaVersions;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.OSClientSession;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.openstack4j.openstack.internal.microversion.MicroVersions;

public class ComputeMicroVersionServiceImpl extends BaseComputeServices implements ComputeMicroVersionService {

    @Override
    public ComputeVersion negotiate() {
        MicroVersionState state = ensureState();
        state.setPinned(null);
        state.setEnabled(true);
        return get();
    }

    @Override
    public ComputeVersion use(String version) {
        MicroVersion requested = MicroVersions.parse(version);
        MicroVersionState state = ensureState();
        MicroVersion lowest = MicroVersions.max(ComputeMicroVersions.MINIMUM, state.getServerMin());
        MicroVersion highest = MicroVersions.min(ComputeMicroVersions.LATEST, state.getServerMax());
        if (requested.compareTo(lowest) < 0 || requested.compareTo(highest) > 0)
            throw new MicroVersionException(String.format(
                    "Compute microversion %s is outside the usable range %s - %s (library %s - %s, server %s - %s)",
                    requested, lowest, highest, ComputeMicroVersions.MINIMUM, ComputeMicroVersions.LATEST,
                    state.getServerMin(), state.getServerMax()));
        state.setPinned(requested);
        state.setEnabled(true);
        return get();
    }

    @Override
    public void clear() {
        MicroVersionState state = ComputeMicroVersions.currentState();
        if (state != null) {
            state.setEnabled(false);
            state.setPinned(null);
        }
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

    private MicroVersionState ensureState() {
        MicroVersionState state = ComputeMicroVersions.currentState();
        if (state != null)
            return state;
        NovaVersions.Entry v21 = new ComputeVersionDiscovery().fetch().v21();
        if (v21 == null || v21.version == null || v21.version.isEmpty())
            throw new MicroVersionException("This compute endpoint does not support microversions (no v2.1 API with a version range)");
        MicroVersion min = v21.minVersion == null || v21.minVersion.isEmpty() ? ComputeMicroVersions.MINIMUM : MicroVersions.parse(v21.minVersion);
        OSClientSession<?, ?> session = OSClientSession.getCurrent();
        return MicroVersionStore.putIfAbsent(session, ComputeMicroVersions.key(session), new MicroVersionState(min, MicroVersions.parse(v21.version)));
    }
}
