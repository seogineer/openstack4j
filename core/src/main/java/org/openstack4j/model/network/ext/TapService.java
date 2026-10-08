package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A TaaS tap service (the port mirrored traffic goes to). Fields without a getter are in getAttributes(). */
public interface TapService extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getPortId();
    String getStatus();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
