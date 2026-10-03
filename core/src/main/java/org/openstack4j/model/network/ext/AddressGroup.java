package org.openstack4j.model.network.ext;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** An address group: a set of CIDRs security group rules can reference. */
public interface AddressGroup extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getProjectId();
    List<String> getAddresses();
}
