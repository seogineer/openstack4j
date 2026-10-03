package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** A network segment (routed provider networks). */
public interface Segment extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getNetworkId();
    String getNetworkType();
    String getPhysicalNetwork();
    Integer getSegmentationId();
    Integer getRevisionNumber();
}
