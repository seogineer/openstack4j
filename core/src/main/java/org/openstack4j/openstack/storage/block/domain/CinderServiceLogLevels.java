package org.openstack4j.openstack.storage.block.domain;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.model.storage.block.ServiceLogLevel;

/** {@code {"log_levels": [...]}} from {@code PUT /os-services/get-log}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderServiceLogLevels implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("log_levels")
    private List<Level> levels;

    public List<Level> getLevels() { return levels == null ? Collections.emptyList() : levels; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Level implements ServiceLogLevel {
        private static final long serialVersionUID = 1L;
        private String binary;
        private String host;
        private Map<String, String> levels;

        @Override public String getBinary() { return binary; }
        @Override public String getHost() { return host; }
        @Override public Map<String, String> getLevels() { return levels; }
    }
}
