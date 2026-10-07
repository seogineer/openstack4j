package org.openstack4j.openstack.baremetal.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.baremetal.Driver;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicDriver implements Driver {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name") private String name;
    @JsonProperty("hosts") private List<String> hosts;
    @JsonProperty("type") private String type;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getName() { return name; }
    @Override public List<String> getHosts() { return hosts; }
    @Override public String getType() { return type; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class IronicDriverList extends ListResult<IronicDriver> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("drivers")
        private List<IronicDriver> list;

        @Override
        protected List<IronicDriver> value() {
            return list;
        }
    }
}
