package org.openstack4j.openstack.dns.v2.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.dns.v2.ext.ReverseFloatingIp;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DesignateReverseFloatingIp implements ReverseFloatingIp {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("ptrdname") private String ptrdname;
    @JsonProperty("description") private String description;
    @JsonProperty("ttl") private Integer ttl;
    @JsonProperty("address") private String address;
    @JsonProperty("status") private String status;
    @JsonProperty("action") private String action;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getPtrdname() { return ptrdname; }
    @Override public String getDescription() { return description; }
    @Override public Integer getTtl() { return ttl; }
    @Override public String getAddress() { return address; }
    @Override public String getStatus() { return status; }
    @Override public String getAction() { return action; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class DesignateReverseFloatingIpList extends ListResult<DesignateReverseFloatingIp> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("floatingips")
        private List<DesignateReverseFloatingIp> list;

        @Override
        protected List<DesignateReverseFloatingIp> value() {
            return list;
        }
    }
}
