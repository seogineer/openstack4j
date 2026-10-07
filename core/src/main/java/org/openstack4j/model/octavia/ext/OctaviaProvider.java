package org.openstack4j.model.octavia.ext;

import org.openstack4j.model.ModelEntity;

/** An Octavia provider driver. */
public interface OctaviaProvider extends ModelEntity {
    String getName();
    String getDescription();
}
