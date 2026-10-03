package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A router conntrack helper (l3-conntrack-helper). */
public interface ConntrackHelper extends ModelEntity {
    String getId();
    String getProtocol();
    Integer getPort();
    String getHelper();
}
