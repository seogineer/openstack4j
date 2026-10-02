package org.openstack4j.openstack.storage.block.domain;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.model.storage.block.BackendCapabilities;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderBackendCapabilities implements BackendCapabilities {

    private static final long serialVersionUID = 1L;

    private String namespace;
    @JsonProperty("vendor_name") private String vendorName;
    @JsonProperty("volume_backend_name") private String volumeBackendName;
    @JsonProperty("pool_name") private String poolName;
    @JsonProperty("driver_version") private String driverVersion;
    @JsonProperty("storage_protocol") private String storageProtocol;
    @JsonProperty("display_name") private String displayName;
    private String description;
    private String visibility;
    @JsonProperty("replication_targets") private List<String> replicationTargets;
    private Map<String, Object> properties;

    @Override public String getNamespace() { return namespace; }
    @Override public String getVendorName() { return vendorName; }
    @Override public String getVolumeBackendName() { return volumeBackendName; }
    @Override public String getPoolName() { return poolName; }
    @Override public String getDriverVersion() { return driverVersion; }
    @Override public String getStorageProtocol() { return storageProtocol; }
    @Override public String getDisplayName() { return displayName; }
    @Override public String getDescription() { return description; }
    @Override public String getVisibility() { return visibility; }
    @Override public List<String> getReplicationTargets() { return replicationTargets; }
    @Override public Map<String, Object> getProperties() { return properties; }
}
