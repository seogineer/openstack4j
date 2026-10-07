package org.openstack4j.model.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A bare metal allocation (a node picked for a resource class and traits). Fields without a getter are in getAttributes(). */
public interface Allocation extends ModelEntity {
    String getUuid();
    String getName();
    String getNodeUuid();
    String getState();
    String getLastError();
    String getResourceClass();
    List<String> getCandidateNodes();
    List<String> getTraits();
    Map<String, Object> getExtra();
    String getOwner();
    String getCreatedAt();
    String getUpdatedAt();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
