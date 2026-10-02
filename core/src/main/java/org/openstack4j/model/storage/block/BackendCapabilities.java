package org.openstack4j.model.storage.block;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Capabilities a volume backend reports ({@code GET /capabilities/{host}}; admin). */
public interface BackendCapabilities extends ModelEntity {
    String getNamespace();
    String getVendorName();
    String getVolumeBackendName();
    String getPoolName();
    String getDriverVersion();
    String getStorageProtocol();
    String getDisplayName();
    String getDescription();
    String getVisibility();
    List<String> getReplicationTargets();
    /** @return the property schema, keyed by property name */
    Map<String, Object> getProperties();
}
