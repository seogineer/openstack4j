package org.openstack4j.model.network.ext;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A port binding on a host (binding-extended); a port has one ACTIVE binding and, during live migration, an INACTIVE one. */
public interface PortBinding extends ModelEntity {
    String getHost();
    String getStatus();
    String getVifType();
    String getVnicType();
    Map<String, Object> getProfile();
    Map<String, Object> getVifDetails();
}
