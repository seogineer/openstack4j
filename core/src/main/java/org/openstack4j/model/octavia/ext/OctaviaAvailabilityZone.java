package org.openstack4j.model.octavia.ext;

import org.openstack4j.model.ModelEntity;

/** An Octavia availability zone, addressed by name. */
public interface OctaviaAvailabilityZone extends ModelEntity {
    String getName();
    String getDescription();
    Boolean isEnabled();
    String getAvailabilityZoneProfileId();
}
