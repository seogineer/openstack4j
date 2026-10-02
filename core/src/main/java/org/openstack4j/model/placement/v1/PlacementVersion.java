package org.openstack4j.model.placement.v1;

import org.openstack4j.model.ModelEntity;

/**
 * The Placement microversion range of the server and the microversion this session uses.
 */
public interface PlacementVersion extends ModelEntity {

    String getServerMinVersion();

    String getServerMaxVersion();

    /** @return the microversion sent with requests, or {@code null} when the server is older than 1.28 */
    String getMicroVersion();

    /** @return {@code true} when the version was set with {@code useMicroVersion} instead of negotiated */
    boolean isPinned();
}
