package org.openstack4j.openstack.placement.v1.internal;

import org.openstack4j.api.placement.v1.VersionService;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.model.placement.v1.PlacementMicroVersions;
import org.openstack4j.model.placement.v1.PlacementVersion;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.placement.v1.domain.PlacementVersionInfo;

public class VersionServiceImpl extends BasePlacementV1Service implements VersionService {

    @Override
    public PlacementVersion get() {
        PlacementSessionState.State state = state();
        String version;
        try {
            version = microVersion().toString();
        } catch (PlacementMicroVersionException tooOld) {
            version = null;
        }
        return new PlacementVersionInfo(state.serverMin.toString(), state.serverMax.toString(), version, state.pinned != null);
    }

    /**
     * Pins the microversion for the current session and endpoint, or returns to negotiation when {@code version} is
     * {@code null}.
     */
    public void pin(String version) {
        PlacementSessionState.State state = state();
        if (version == null) {
            state.pinned = null;
            return;
        }
        MicroVersion requested;
        try {
            requested = new MicroVersion(version);
        } catch (IllegalArgumentException e) {
            throw new PlacementMicroVersionException("Invalid placement microversion '" + version + "': " + e.getMessage());
        }
        MicroVersion lowest = max(PlacementMicroVersions.MINIMUM, state.serverMin);
        MicroVersion highest = min(PlacementMicroVersions.LATEST, state.serverMax);
        if (requested.compareTo(lowest) < 0 || requested.compareTo(highest) > 0)
            throw new PlacementMicroVersionException(String.format(
                    "Placement microversion %s is outside the usable range %s - %s (library %s - %s, server %s - %s)",
                    requested, lowest, highest, PlacementMicroVersions.MINIMUM, PlacementMicroVersions.LATEST,
                    state.serverMin, state.serverMax));
        state.pinned = requested;
    }
}
