package org.openstack4j.model.placement.v1;

import java.util.List;

import org.openstack4j.model.ModelEntity;

public interface ResourceProviderAggregates extends ModelEntity {

    long getResourceProviderGeneration();

    /** @return aggregate UUIDs the provider belongs to */
    List<String> getAggregates();
}
