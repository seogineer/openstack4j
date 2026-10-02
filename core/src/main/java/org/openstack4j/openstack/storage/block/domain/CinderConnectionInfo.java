package org.openstack4j.openstack.storage.block.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;

/** {@code {"connection_info": {...}}} as returned by {@code os-initialize_connection}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderConnectionInfo implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("connection_info")
    private Map<String, Object> connectionInfo;

    public Map<String, Object> getConnectionInfo() {
        return connectionInfo;
    }
}
