package org.openstack4j.openstack.storage.block.domain;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.model.storage.block.ReplicationTarget;

/** {@code {"replication_targets": [...]}} from the {@code list_replication_targets} group action. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderReplicationTargets implements ModelEntity {
    private static final long serialVersionUID = 1L;
    @JsonProperty("replication_targets")
    private List<Target> targets;
    public List<Target> getTargets() { return targets == null ? Collections.emptyList() : targets; }

    public static class Target implements ReplicationTarget {
        private static final long serialVersionUID = 1L;
        @JsonProperty("backend_id") private String backendId;
        private final Map<String, String> properties = new LinkedHashMap<>();
        @JsonAnySetter void put(String key, Object value) { properties.put(key, value == null ? null : String.valueOf(value)); }
        @Override public String getBackendId() { return backendId; }
        @Override public Map<String, String> getProperties() { return properties; }
    }
}
