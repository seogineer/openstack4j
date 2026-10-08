package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A TaaS tap mirror (a port mirrored to a remote IP over GRE or ERSPAN). Fields without a getter are in getAttributes(). */
public interface TapMirror extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    String getPortId();
    Map<String, Object> getDirections();
    String getRemoteIp();
    String getMirrorType();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
