package org.openstack4j.model.placement.v1;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

public interface ResourceProviderUsages extends ModelEntity {

    long getResourceProviderGeneration();

    /** @return used amount per resource class */
    Map<String, Long> getUsages();
}
