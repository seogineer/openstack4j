package org.openstack4j.model.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal driver (hardware type). Fields without a getter are in getAttributes(). */
public interface Driver extends ModelEntity {
    String getName();
    List<String> getHosts();
    String getType();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
