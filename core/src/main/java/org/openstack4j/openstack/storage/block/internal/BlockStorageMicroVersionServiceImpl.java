package org.openstack4j.openstack.storage.block.internal;

import org.openstack4j.api.storage.BlockStorageMicroVersionService;
import org.openstack4j.model.storage.block.BlockStorageVersion;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.internal.microversion.VersionRange;
import org.openstack4j.openstack.storage.block.domain.CinderBlockStorageVersion;
import org.openstack4j.openstack.storage.block.domain.CinderVersions;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.openstack.internal.OSClientSession;

public class BlockStorageMicroVersionServiceImpl extends BaseBlockStorageServices implements BlockStorageMicroVersionService {

    @Override
    public BlockStorageVersion negotiate() {
        BlockStorageMicroVersions.SUPPORT.negotiate(BlockStorageMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public BlockStorageVersion use(String version) {
        BlockStorageMicroVersions.SUPPORT.use(version, BlockStorageMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public void clear() {
        BlockStorageMicroVersions.SUPPORT.clear();
    }

    @Override
    public BlockStorageVersion get() {
        MicroVersionState state = BlockStorageMicroVersions.currentState();
        if (state == null)
            return new CinderBlockStorageVersion(null, null, null, false, false);
        MicroVersion effective = effectiveMicroVersion(null);
        return new CinderBlockStorageVersion(state.getServerMin().toString(), state.getServerMax().toString(),
                effective == null ? null : effective.toString(), state.getPinned() != null, state.isEnabled());
    }

    /** @return the v3 range from the Cinder root document, or {@code null} when the server has none */
    private static VersionRange discover() {
        String endpoint = OSClientSession.getCurrent().getEndpoint(ServiceType.BLOCK_STORAGE);
        if (endpoint == null || !endpoint.matches(".*/v3(/.*)?$"))
            throw new MicroVersionException("Block storage microversions need a v3 endpoint, but the catalog resolved " + endpoint
                    + " (type volume/volumev2). Use a cloud or endpoint resolver that offers volumev3.");
        CinderVersions.Entry v3 = new BlockStorageVersionDiscovery().fetch().v3();
        if (v3 == null || v3.version == null || v3.version.isEmpty())
            return null;
        MicroVersion min = v3.minVersion == null || v3.minVersion.isEmpty() ? null : MicroVersions.parse(v3.minVersion);
        return new VersionRange(min, MicroVersions.parse(v3.version));
    }
}
