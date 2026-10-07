package org.openstack4j.model.octavia.ext;

import org.openstack4j.model.ModelEntity;

/** An Octavia availability zone profile: provider-specific zone data (a JSON string). */
public interface AvailabilityZoneProfile extends ModelEntity {
    String getId();
    String getName();
    String getProviderName();
    String getAvailabilityZoneData();
}
