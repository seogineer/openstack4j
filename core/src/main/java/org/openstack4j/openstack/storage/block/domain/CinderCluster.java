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
import org.openstack4j.model.storage.block.StorageCluster;

@JsonRootName("cluster")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderCluster implements StorageCluster {

    private static final long serialVersionUID = 1L;

    private String name;
    private String binary;
    private String state;
    private String status;
    @JsonProperty("disabled_reason") private String disabledReason;
    @JsonProperty("num_hosts") private Integer numHosts;
    @JsonProperty("num_down_hosts") private Integer numDownHosts;
    @JsonProperty("last_heartbeat") private Date lastHeartbeat;
    @JsonProperty("created_at") private Date createdAt;
    @JsonProperty("updated_at") private Date updatedAt;
    @JsonProperty("replication_status") private String replicationStatus;
    private Boolean frozen;
    @JsonProperty("active_backend_id") private String activeBackendId;

    @Override public String getName() { return name; }
    @Override public String getBinary() { return binary; }
    @Override public String getState() { return state; }
    @Override public String getStatus() { return status; }
    @Override public String getDisabledReason() { return disabledReason; }
    @Override public Integer getNumHosts() { return numHosts; }
    @Override public Integer getNumDownHosts() { return numDownHosts; }
    @Override public Date getLastHeartbeat() { return lastHeartbeat; }
    @Override public Date getCreatedAt() { return createdAt; }
    @Override public Date getUpdatedAt() { return updatedAt; }
    @Override public String getReplicationStatus() { return replicationStatus; }
    @Override public Boolean getFrozen() { return frozen; }
    @Override public String getActiveBackendId() { return activeBackendId; }

    public static class Clusters extends ListResult<CinderCluster> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("clusters")
        private List<CinderCluster> items;

        @Override
        protected List<CinderCluster> value() {
            return items;
        }
    }
}
