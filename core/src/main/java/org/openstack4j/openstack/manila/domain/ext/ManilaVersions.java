package org.openstack4j.openstack.manila.domain.ext;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Manila root document: {@code {"versions": [{"id": "v2.0", "version": "2.99", "min_version": "2.0", ...}]}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaVersions implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("versions")
    private List<Entry> versions;

    /** @return the v2 entry, or {@code null} when the server has no v2 API */
    public Entry v2() {
        if (versions != null)
            for (Entry e : versions)
                if (e.id != null && e.id.startsWith("v2"))
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
