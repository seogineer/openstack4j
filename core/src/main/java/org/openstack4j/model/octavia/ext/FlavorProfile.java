package org.openstack4j.model.octavia.ext;

import org.openstack4j.model.ModelEntity;

/** An Octavia flavor profile: provider-specific flavor data (a JSON string). */
public interface FlavorProfile extends ModelEntity {
    String getId();
    String getName();
    String getProviderName();
    String getFlavorData();
}
