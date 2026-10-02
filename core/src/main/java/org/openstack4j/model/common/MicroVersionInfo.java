package org.openstack4j.model.common;

import org.openstack4j.model.ModelEntity;

/** The microversion range of a service endpoint and the microversion this session sends to it. */
public interface MicroVersionInfo extends ModelEntity {

    /** @return the server's minimum microversion, or {@code null} before any discovery */
    String getServerMinVersion();

    String getServerMaxVersion();

    /** @return the microversion sent with requests, or {@code null} when microversions are off */
    String getMicroVersion();

    boolean isPinned();

    boolean isEnabled();
}
