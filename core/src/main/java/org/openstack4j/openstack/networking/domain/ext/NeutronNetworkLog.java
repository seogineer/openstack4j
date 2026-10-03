package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.NetworkLog;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("log")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronNetworkLog implements NetworkLog {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("enabled") private Boolean enabled;
    @JsonProperty("resource_type") private String resourceType;
    @JsonProperty("resource_id") private String resourceId;
    @JsonProperty("target_id") private String targetId;
    @JsonProperty("event") private String event;
    @JsonProperty("revision_number") private Integer revisionNumber;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getProjectId() { return projectId; }
    @Override public Boolean isEnabled() { return enabled; }
    @Override public String getResourceType() { return resourceType; }
    @Override public String getResourceId() { return resourceId; }
    @Override public String getTargetId() { return targetId; }
    @Override public String getEvent() { return event; }
    @Override public Integer getRevisionNumber() { return revisionNumber; }

    public static class Logs extends ListResult<NeutronNetworkLog> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("logs")
        private List<NeutronNetworkLog> list;

        @Override
        protected List<NeutronNetworkLog> value() {
            return list;
        }
    }
}
