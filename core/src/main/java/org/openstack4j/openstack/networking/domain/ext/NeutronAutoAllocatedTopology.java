package org.openstack4j.openstack.networking.domain.ext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.AutoAllocatedTopology;

@JsonRootName("auto_allocated_topology")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronAutoAllocatedTopology implements AutoAllocatedTopology {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("dry-run") private String dryRun;

    @Override public String getId() { return id; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getDryRun() { return dryRun; }
}
