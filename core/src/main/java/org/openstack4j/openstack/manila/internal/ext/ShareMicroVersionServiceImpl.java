package org.openstack4j.openstack.manila.internal.ext;

import org.openstack4j.api.manila.ShareMicroVersionService;
import org.openstack4j.model.manila.ShareApiVersion;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.internal.microversion.VersionRange;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareApiVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaVersions;

public class ShareMicroVersionServiceImpl extends BaseManilaExtService implements ShareMicroVersionService {

    @Override
    public ShareApiVersion negotiate() {
        ManilaMicroVersions.SUPPORT.negotiate(ShareMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public ShareApiVersion use(String version) {
        ManilaMicroVersions.SUPPORT.use(version, ShareMicroVersionServiceImpl::discover);
        return get();
    }

    @Override
    public void clear() {
        ManilaMicroVersions.SUPPORT.clear();
    }

    @Override
    public ShareApiVersion get() {
        MicroVersionState state = ManilaMicroVersions.currentState();
        if (state == null)
            return new ManilaShareApiVersion(null, null, null, false, false);
        MicroVersion effective = ManilaMicroVersions.SUPPORT.effective(null, null);
        return new ManilaShareApiVersion(state.getServerMin().toString(), state.getServerMax().toString(),
                effective == null ? null : effective.toString(), state.getPinned() != null, state.isEnabled());
    }

    /** @return the v2 range from the Manila root document, or {@code null} when the server has none */
    private static VersionRange discover() {
        ManilaVersions root = new ManilaVersionDiscovery().fetch();
        ManilaVersions.Entry v2 = root == null ? null : root.v2();
        if (v2 == null || v2.version == null || v2.version.isEmpty())
            return null;
        MicroVersion min = v2.minVersion == null || v2.minVersion.isEmpty() ? null : MicroVersions.parse(v2.minVersion);
        return new VersionRange(min, MicroVersions.parse(v2.version));
    }
}
