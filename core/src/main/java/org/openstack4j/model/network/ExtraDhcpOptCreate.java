package org.openstack4j.model.network;

import org.openstack4j.common.Buildable;
import org.openstack4j.model.network.builder.ExtraDhcpOptBuilder;

/**
 * @author Ales Kemr
 */
public interface ExtraDhcpOptCreate extends Buildable<ExtraDhcpOptBuilder> {

    String getOptValue();

    String getOptName();

    /** @return the IP version (4 or 6) the option applies to, or null when not set (Neutron then uses 4) */
    Integer getIpVersion();
}
