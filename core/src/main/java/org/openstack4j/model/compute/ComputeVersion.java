package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/** The compute microversion range of the server and the microversion this session sends. */
public interface ComputeVersion extends ModelEntity {

    /** @return the server's minimum microversion, or {@code null} before any discovery */
    String getServerMinVersion();

    String getServerMaxVersion();

    /** @return the microversion sent with requests, or {@code null} when microversions are off */
    String getMicroVersion();

    boolean isPinned();

    boolean isEnabled();
}
