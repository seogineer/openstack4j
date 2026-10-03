package org.openstack4j.model.network.ext;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** A subnet pool: prefixes subnets are allocated from. */
public interface SubnetPool extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getProjectId();
    List<String> getPrefixes();
    Integer getDefaultPrefixlen();
    Integer getMinPrefixlen();
    Integer getMaxPrefixlen();
    Integer getDefaultQuota();
    String getAddressScopeId();
    Integer getIpVersion();
    Boolean isShared();
    Boolean isDefault();
    Integer getRevisionNumber();
    List<String> getTags();
}
