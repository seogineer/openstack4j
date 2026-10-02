package org.openstack4j.openstack.storage.block.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Cinder root document: {@code {"versions": [{"id": "v3.0", "version": "3.71", "min_version": "3.0", ...}]}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderVersions implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("versions")
    private List<Entry> versions;

    /** @return the v3 entry, or {@code null} when the server has no v3 API */
    public Entry v3() {
        if (versions != null)
            for (Entry e : versions)
                if (e.id != null && e.id.startsWith("v3"))
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
