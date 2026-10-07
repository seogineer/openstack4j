package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.VolumeConnector;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicVolumeConnector implements VolumeConnector {

    private static final long serialVersionUID = 1L;

    @JsonProperty("uuid") private String uuid;
    @JsonProperty("node_uuid") private String nodeUuid;
    @JsonProperty("type") private String type;
    @JsonProperty("connector_id") private String connectorId;
    @JsonProperty("extra") private Map<String, Object> extra;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getUuid() { return uuid; }
    @Override public String getNodeUuid() { return nodeUuid; }
    @Override public String getType() { return type; }
    @Override public String getConnectorId() { return connectorId; }
    @Override public Map<String, Object> getExtra() { return extra; }
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

    public static class IronicVolumeConnectorList extends ListResult<IronicVolumeConnector> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("connectors")
        private List<IronicVolumeConnector> list;

        @Override
        protected List<IronicVolumeConnector> value() {
            return list;
        }
    }
}
