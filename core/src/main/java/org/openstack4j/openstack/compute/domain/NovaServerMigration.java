package org.openstack4j.openstack.compute.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.compute.ServerMigration;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("migration")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaServerMigration implements ServerMigration {

    private static final long serialVersionUID = 1L;

    private String id;
    private String uuid;
    @JsonProperty("server_uuid") private String serverUuid;
    private String status;
    @JsonProperty("source_compute") private String sourceCompute;
    @JsonProperty("source_node") private String sourceNode;
    @JsonProperty("dest_compute") private String destCompute;
    @JsonProperty("dest_node") private String destNode;
    @JsonProperty("dest_host") private String destHost;
    @JsonProperty("memory_total_bytes") private Long memoryTotalBytes;
    @JsonProperty("memory_processed_bytes") private Long memoryProcessedBytes;
    @JsonProperty("memory_remaining_bytes") private Long memoryRemainingBytes;
    @JsonProperty("disk_total_bytes") private Long diskTotalBytes;
    @JsonProperty("disk_processed_bytes") private Long diskProcessedBytes;
    @JsonProperty("disk_remaining_bytes") private Long diskRemainingBytes;
    @JsonProperty("created_at") private Date createdAt;
    @JsonProperty("updated_at") private Date updatedAt;
    @JsonProperty("user_id") private String userId;
    @JsonProperty("project_id") private String projectId;

    @Override public String getId() { return id; }
    @Override public String getUuid() { return uuid; }
    @Override public String getServerUuid() { return serverUuid; }
    @Override public String getStatus() { return status; }
    @Override public String getSourceCompute() { return sourceCompute; }
    @Override public String getSourceNode() { return sourceNode; }
    @Override public String getDestCompute() { return destCompute; }
    @Override public String getDestNode() { return destNode; }
    @Override public String getDestHost() { return destHost; }
    @Override public Long getMemoryTotalBytes() { return memoryTotalBytes; }
    @Override public Long getMemoryProcessedBytes() { return memoryProcessedBytes; }
    @Override public Long getMemoryRemainingBytes() { return memoryRemainingBytes; }
    @Override public Long getDiskTotalBytes() { return diskTotalBytes; }
    @Override public Long getDiskProcessedBytes() { return diskProcessedBytes; }
    @Override public Long getDiskRemainingBytes() { return diskRemainingBytes; }
    @Override public Date getCreatedAt() { return createdAt; }
    @Override public Date getUpdatedAt() { return updatedAt; }
    @Override public String getUserId() { return userId; }
    @Override public String getProjectId() { return projectId; }

    public static class NovaServerMigrations extends ListResult<NovaServerMigration> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("migrations")
        private List<NovaServerMigration> migrations;

        @Override
        protected List<NovaServerMigration> value() {
            return migrations;
        }
    }
}
