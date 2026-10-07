package org.openstack4j.openstack.heat.domain.ext;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.heat.ext.StackSnapshot;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("snapshot")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeatStackSnapshot implements StackSnapshot {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("action") private String action;
    @JsonProperty("status") private String status;
    @JsonProperty("status_reason") private String statusReason;
    @JsonProperty("creation_time") private String creationTime;
    @JsonProperty("data") private Map<String, Object> data;

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getAction() { return action; }
    @Override public String getStatus() { return status; }
    @Override public String getStatusReason() { return statusReason; }
    @Override public String getCreationTime() { return creationTime; }
    @Override public Map<String, Object> getData() { return data; }

    public static class Snapshots extends ListResult<HeatStackSnapshot> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("snapshots")
        private List<HeatStackSnapshot> list;

        @Override
        protected List<HeatStackSnapshot> value() {
            return list;
        }
    }
}
