package org.openstack4j.model.compute;

import org.openstack4j.model.ModelEntity;

/** A remote console URL (2.6+). */
public interface RemoteConsole extends ModelEntity {
    String getProtocol();
    String getType();
    String getUrl();
}
