package org.openstack4j.openstack.reservation.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.reservation.Lease;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("lease")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlazarLease implements Lease {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("start_date") private String startDate;
    @JsonProperty("end_date") private String endDate;
    @JsonProperty("status") private String status;
    @JsonProperty("degraded") private Boolean degraded;
    @JsonProperty("user_id") private String userId;
    @JsonProperty("project_id") private String projectId;
    @JsonProperty("trust_id") private String trustId;
    @JsonProperty("reservations") private List<Map<String, Object>> reservations;
    @JsonProperty("events") private List<Map<String, Object>> events;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getStartDate() { return startDate; }
    @Override public String getEndDate() { return endDate; }
    @Override public String getStatus() { return status; }
    @Override public Boolean isDegraded() { return degraded; }
    @Override public String getUserId() { return userId; }
    @Override public String getProjectId() { return projectId; }
    @Override public String getTrustId() { return trustId; }
    @Override public List<Map<String, Object>> getReservations() { return reservations; }
    @Override public List<Map<String, Object>> getEvents() { return events; }
    @Override public String getCreatedAt() { return createdAt; }
    @Override public String getUpdatedAt() { return updatedAt; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class BlazarLeaseList extends ListResult<BlazarLease> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("leases")
        private List<BlazarLease> list;

        @Override
        protected List<BlazarLease> value() {
            return list;
        }
    }
}
