package org.openstack4j.model.storage.block;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A volume type associated with a QoS specification. */
public interface QosAssociation extends ModelEntity {
    String getAssociationType();
    String getName();
    String getId();
}
