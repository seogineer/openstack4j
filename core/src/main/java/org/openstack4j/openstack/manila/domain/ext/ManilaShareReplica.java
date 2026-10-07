package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.manila.ext.ShareReplica;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share_replica")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareReplica implements ShareReplica {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("share_id") private String shareId;
    @JsonProperty("status") private String status;
    @JsonProperty("replica_state") private String replicaState;
    @JsonProperty("availability_zone") private String availabilityZone;
    @JsonProperty("share_network_id") private String shareNetworkId;
    @JsonProperty("share_server_id") private String shareServerId;
    @JsonProperty("host") private String host;
    @JsonProperty("cast_rules_to_readonly") private Boolean castRulesToReadonly;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getShareId() { return shareId; }
    @Override public String getStatus() { return status; }
    @Override public String getReplicaState() { return replicaState; }
    @Override public String getAvailabilityZone() { return availabilityZone; }
    @Override public String getShareNetworkId() { return shareNetworkId; }
    @Override public String getShareServerId() { return shareServerId; }
    @Override public String getHost() { return host; }
    @Override public Boolean isCastRulesToReadonly() { return castRulesToReadonly; }
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

    public static class ManilaShareReplicaList extends ListResult<ManilaShareReplica> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("share_replicas")
        private List<ManilaShareReplica> list;

        @Override
        protected List<ManilaShareReplica> value() {
            return list;
        }
    }
}
