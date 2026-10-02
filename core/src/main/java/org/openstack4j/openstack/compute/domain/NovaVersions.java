package org.openstack4j.openstack.compute.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Nova root document: {@code {"versions": [{"id": "v2.1", "version": "2.100", "min_version": "2.1"}, ...]}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaVersions implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("versions")
    private List<Entry> versions;
    @JsonProperty("version")
    private Entry version;

    /** @return the v2.1 entry, or {@code null} when the server has no microversion-capable API */
    public Entry v21() {
        if (version != null && "v2.1".equals(version.id))
            return version;
        if (versions != null)
            for (Entry e : versions)
                if ("v2.1".equals(e.id))
                    return e;
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("id")
        public String id;
        @JsonProperty("version")
        public String version;
        @JsonProperty("min_version")
        public String minVersion;
    }
}
