package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A rule template copied into new (default and/or non-default) security groups. */
public interface DefaultSecurityGroupRule extends ModelEntity {
    String getId();
    String getDirection();
    String getEthertype();
    String getProtocol();
    Integer getPortRangeMin();
    Integer getPortRangeMax();
    String getRemoteIpPrefix();
    String getRemoteGroupId();
    String getRemoteAddressGroupId();
    String getDescription();
    Boolean getUsedInDefaultSg();
    Boolean getUsedInNonDefaultSg();
}
