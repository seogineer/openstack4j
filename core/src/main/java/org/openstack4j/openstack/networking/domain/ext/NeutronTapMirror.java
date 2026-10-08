package org.openstack4j.openstack.networking.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.TapMirror;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("tap_mirror")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronTapMirror implements TapMirror {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("port_id") private String portId;
    @JsonProperty("directions") private Map<String, Object> directions;
    @JsonProperty("remote_ip") private String remoteIp;
    @JsonProperty("mirror_type") private String mirrorType;
    @JsonProperty("project_id") private String projectId;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public String getPortId() { return portId; }
    @Override public Map<String, Object> getDirections() { return directions; }
    @Override public String getRemoteIp() { return remoteIp; }
    @Override public String getMirrorType() { return mirrorType; }
    @Override public String getProjectId() { return projectId; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class NeutronTapMirrorList extends ListResult<NeutronTapMirror> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("tap_mirrors")
        private List<NeutronTapMirror> list;

        @Override
        protected List<NeutronTapMirror> value() {
            return list;
        }
    }
}
