package org.openstack4j.model.octavia.ext;

import org.openstack4j.model.ModelEntity;

/** Traffic statistics of one listener on an amphora. */
public interface AmphoraStats extends ModelEntity {
    String getId();
    String getListenerId();
    String getLoadbalancerId();
    Long getActiveConnections();
    Long getBytesIn();
    Long getBytesOut();
    Long getRequestErrors();
    Long getTotalConnections();
}
