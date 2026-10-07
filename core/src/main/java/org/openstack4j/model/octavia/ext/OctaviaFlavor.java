package org.openstack4j.model.octavia.ext;

import org.openstack4j.model.ModelEntity;

/** An Octavia flavor: a named load balancer configuration backed by a flavor profile. */
public interface OctaviaFlavor extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    Boolean isEnabled();
    String getFlavorProfileId();
}
