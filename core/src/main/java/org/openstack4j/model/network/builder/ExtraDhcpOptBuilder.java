package org.openstack4j.model.network.builder;

import org.openstack4j.common.Buildable.Builder;
import org.openstack4j.model.network.ExtraDhcpOptCreate;

/**
 * @author Ales Kemr
 */
public interface ExtraDhcpOptBuilder extends Builder<ExtraDhcpOptBuilder, ExtraDhcpOptCreate> {

    ExtraDhcpOptBuilder optValue(String optValue);

    ExtraDhcpOptBuilder optName(String optName);

    /** @param ipVersion 4 or 6; null (default) leaves {@code ip_version} out of the request and Neutron uses 4 */
    ExtraDhcpOptBuilder ipVersion(Integer ipVersion);

}
