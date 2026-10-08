package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A TaaS tap flow (a source port mirrored to a tap service). Fields without a getter are in getAttributes(). */
public interface TapFlow extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getTapServiceId();
    String getSourcePort();
    String getDirection();
    String getVlanFilter();
    String getStatus();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
