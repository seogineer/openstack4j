package org.openstack4j.openstack.baremetal.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Ironic root document: {@code {"versions": [{"id": "v1", "version": "1.96", "min_version": "1.1", ...}]}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IronicVersions implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("versions")
    private List<Entry> versions;

    /** @return the v1 entry, or {@code null} when the server has no v1 API */
    public Entry v1() {
        if (versions != null)
            for (Entry e : versions)
                if (e.id != null && e.id.startsWith("v1"))
                    return e;
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("id")
        public String id;
        @JsonProperty("status")
        public String status;
        @JsonProperty("version")
        public String version;
        @JsonProperty("min_version")
        public String minVersion;
    }
}
