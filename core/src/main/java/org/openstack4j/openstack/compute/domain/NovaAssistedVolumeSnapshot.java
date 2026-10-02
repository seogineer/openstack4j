package org.openstack4j.openstack.compute.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.compute.AssistedVolumeSnapshot;

@JsonRootName("snapshot")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaAssistedVolumeSnapshot implements AssistedVolumeSnapshot {

    private static final long serialVersionUID = 1L;

    private String id;
    private String volumeId;

    @Override public String getId() { return id; }
    @Override public String getVolumeId() { return volumeId; }
}
