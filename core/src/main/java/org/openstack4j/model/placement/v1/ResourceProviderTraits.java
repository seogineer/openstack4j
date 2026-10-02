package org.openstack4j.model.placement.v1;

import java.util.List;

import org.openstack4j.model.ModelEntity;

public interface ResourceProviderTraits extends ModelEntity {

    long getResourceProviderGeneration();

    List<String> getTraits();
}
